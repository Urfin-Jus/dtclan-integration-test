package specifications.modefinitions

import java.util.concurrent.atomic.AtomicInteger

import static specifications.utils.MetricDictionary.getExpectedSampleCountByMetricId
import static specifications.utils.MetricDictionary.getMetricDetails

class FREERADIUS_MO extends MONITORED_OBJECT{

	FREERADIUS_MO(String id, String uuid,String name){
        mo_type = 'freeradius'
        expected_metrics = ["9148994923851888944","9148994929951890589","9148994933551891118",
                            "9148994937951892221","9148994941051892623","9148994944251893284",
                            "9148994948751894048","9148994954151895218","9148994957351895705",
                            "9148994960551896535","9148994964951897206","9148994968451897875",
                            "9148994971451898355","9148994975151898917","9148994978051899293",
                            "9148994981551899849","9148777916851236947","9148777928151239648",
                            "9148777906251234947","9148777909351235381","9148777911151235672",
                            "9149460807051379057"]
        mo_id = id
        mo_uuid = uuid
        mo_name = name
        status = false
        actual_sample_encounters = [:]
        actual_unrelated_encounters = [:]
        unrelated_metrics =[:]
        expected_total_sample_count = 0
        expected_metrics_details = getMetricDetails(expected_metrics)
        expected_metrics.each{ expected_total_sample_count += getExpectedSampleCountByMetricId(it)}
        expected_metrics.each{ actual_sample_encounters.put(it, new AtomicInteger())}
	}

}