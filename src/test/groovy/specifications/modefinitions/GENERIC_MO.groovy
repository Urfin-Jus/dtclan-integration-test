package specifications.modefinitions

import static specifications.utils.MetricDictionary.*
import java.util.concurrent.atomic.AtomicInteger



class GENERIC_MO extends MONITORED_OBJECT {

    static List<String> allowed_type = ["freeradius-database","nec-cwp-business-logic","nec-cwp-database", "nec-cwp-haproxy", "nec-cwp-redis"]

    GENERIC_MO(String id, String uuid,String type, String name){
        mo_type = type
        expected_metrics = ["9148777916851236947","9148777928151239648","9148777906251234947",
                            "9148777909351235381","9148777911151235672","9149460807051379057"]
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