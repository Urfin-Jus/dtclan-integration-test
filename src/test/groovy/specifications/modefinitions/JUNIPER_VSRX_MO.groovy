package specifications.modefinitions

import java.util.concurrent.atomic.AtomicInteger

import static specifications.utils.MetricDictionary.getExpectedSampleCountByMetricId
import static specifications.utils.MetricDictionary.getMetricDetails

class JUNIPER_VSRX_MO  extends MONITORED_OBJECT {

	JUNIPER_VSRX_MO(String id,String uuid,String name){
        mo_type = 'juniper-vsrx'
        expected_metrics = ["9146886307051157430","9146886313251157553","9146886318151157647",
                            "9146886324051157780","9147377659851475509","9146886335151157909",
                            "9146886341951158008","9146886348151158130","9146886353551158240",
                            "9149464936551562662","9149464938751562826","9149464941151563340",
                            "9149464944251563590","9146886361651158362","9146886367551158469",
                            "9146886577951159727","9146886587751159928","9146886592451160040",
                            "9146886723651977428","9146886734351977679","9146886741051977812",
                            "9146886747351977917","9146886756751978074","9146886763551978183",
                            "9146886769851978451"]
        mo_id = id
        mo_uuid = uuid
        mo_name = name
        status = false
        actual_sample_encounters = [:]
        actual_unrelated_encounters = [:]
        unrelated_metrics =[:]
        expected_total_sample_count = 0
        expected_metrics_details= getMetricDetails(expected_metrics)
        expected_metrics.each{ expected_total_sample_count += getExpectedSampleCountByMetricId(it)}
        expected_metrics.each{ actual_sample_encounters.put(it, new AtomicInteger())}
	}

}