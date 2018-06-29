package specifications.modefinitions

import java.util.concurrent.atomic.AtomicInteger
import static specifications.utils.MetricDictionary.*

class VM_MO extends MONITORED_OBJECT {

	VM_MO(String id,String uuid, String name){
        mo_type = 'vm'
        expected_metrics = ["9146523577551079863"]
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