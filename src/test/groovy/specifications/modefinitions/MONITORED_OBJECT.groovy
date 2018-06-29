package specifications.modefinitions

import groovy.json.JsonOutput
import java.util.concurrent.atomic.AtomicInteger
import static specifications.utils.Alfred.*
import static specifications.utils.MetricDictionary.getExpectedMetricName
import static specifications.utils.MetricDictionary.getMetricDetails

class MONITORED_OBJECT {
    //Characteristics
    protected List<String> expected_metrics
    protected Map<String, List> expected_metrics_details
    protected Integer expected_total_sample_count


    // MO RUNTIME DYNAMIC PARAMETERS
    protected String mo_type
    protected String mo_id
    protected String mo_uuid
    protected String mo_name
    protected Map<String,AtomicInteger> actual_sample_encounters
    protected Map<String,AtomicInteger> actual_unrelated_encounters
    protected Map<String,String> unrelated_metrics

    //OUTCOME RESULTS KEEPERS
    protected Boolean is_actual_total_count_passed
    protected Boolean is_all_expected_samples_recieved
    protected Boolean no_unrelated_metric_samples
    protected Boolean status

    // MO SELF-VALIDATION
    protected Boolean isActualTotalCount_Passed(){ if( calculateTotalMetricCount() == expected_total_sample_count ) true  else false}

   // protected Boolean isActualSampleCount_Passed(String metric_id){ if(getActualSampleCountByMetricId(metric_id) == expected_metrics_details.get(metric_id).getAt(1)) true else false }

    protected Boolean noUnrelatedMetricSamples(){ if ( actual_unrelated_encounters && unrelated_metrics) false else true }


    protected Boolean isAllExpectedSamplesRecieved(){ if(prettyExpectedSamplesStatistics() == prettyActualSamplesStatistics()) true else false }

    protected void validate(){
        is_actual_total_count_passed = isActualTotalCount_Passed()
        is_all_expected_samples_recieved = isAllExpectedSamplesRecieved()
        no_unrelated_metric_samples = noUnrelatedMetricSamples()
        status = is_actual_total_count_passed && is_all_expected_samples_recieved && no_unrelated_metric_samples
    }

    protected String getReport(){

     """################################################################################
        MONITORED OBJECT: $mo_id # TYPE: $mo_type # NAME: $mo_name # STATUS: ${status.toString().toUpperCase()}
        ################################################################################
        ACTUAL METRIC SAMPLES ENCOUNTERED:      
        ${prettyActualSamplesStatistics()}
        EXPECTED METRIC SAMPLES:                
        ${prettyExpectedSamplesStatistics()}
        UNRELATED METRIC SAMPLES ENCOUNTERED: ${prettyActualUnrelatedStatistics()}
        VERIFICATION:
            * Total Discovered Samples as Expected?               ${ is_all_expected_samples_recieved.toString().toUpperCase() }  |  ${calculateTotalMetricCount()}  vs $expected_total_sample_count
            * There is no unexpected metric samples discovered?   ${ no_unrelated_metric_samples.toString().toUpperCase() }
            * Discovered Samples per Metric as Expected?          ${ is_actual_total_count_passed.toString().toUpperCase()}       | 
        SAMPLE DIFFERENCE:
        ${prettyActualSamplesDifference() ?: 'N/A'}  
        ______________________________________________________________"""
    }



    // MO Utils
    protected Integer calculateTotalMetricCount(){ Integer count = 0; actual_sample_encounters.each{ count += it.value.toInteger()}; count }

    //protected Integer getActualSampleCountByMetricId(String metric_id){ actual_sample_encounters.find{ it.key == metric_id}.value.toInteger()}

    protected Integer getExpectedSampleCount(String metric_id){ (Integer) expected_metrics_details.get(metric_id).getAt(1) }

    protected String prettyExpectedSamplesStatistics(){
        Map<String, Integer> prettyMap =[:]
        expected_metrics.each{
            prettyMap.put( (String) expected_metrics_details.get(it).getAt(0), getExpectedSampleCount(it))
        }
        JsonOutput.prettyPrint(JsonOutput.toJson(prettyMap)) ?: 'Absent'
    }

    protected String prettyActualSamplesStatistics(){
        Map<String, Integer> prettyMap =[:]
        actual_sample_encounters.each{
            prettyMap.put(expected_metrics_details.(it.key).getAt(0),it.value.toInteger())
        }
        JsonOutput.prettyPrint(JsonOutput.toJson(prettyMap)) ?: 'Absent'
    }

    protected String prettyActualUnrelatedStatistics(){
        Map<String, Integer> prettyMap =[:]

        if (actual_unrelated_encounters || unrelated_metrics) {
            actual_unrelated_encounters.each {
                prettyMap.put(unrelated_metrics[it.key], it.value.toInteger())
            }
            JsonOutput.prettyPrint(JsonOutput.toJson(prettyMap)) ?: 'N/A'
        }
    }

    protected String prettyActualSamplesDifference(){
        Map<String, Integer> prettyMap_actual =[:]
        Map<String, Integer> prettyMap_expected =[:]
        Map<String, Map> prettyMap_returned =[:]

        expected_metrics.each{
            prettyMap_expected.put( (String) expected_metrics_details.get(it).getAt(0), getExpectedSampleCount(it))
        }

        actual_sample_encounters.each{
            prettyMap_actual.put(expected_metrics_details.(it.key).getAt(0),it.value.toInteger())
        }

        def act_minus_exp = prettyMap_actual.minus(prettyMap_expected)
        def exp_minus_act = prettyMap_expected.minus(prettyMap_actual)
        if (act_minus_exp || exp_minus_act){
            prettyMap_returned.put('Actual',act_minus_exp)
            prettyMap_returned.put('Expected',exp_minus_act )
            JsonOutput.prettyPrint(JsonOutput.toJson(prettyMap_returned))
        } else return 'N/A'
    }



}
