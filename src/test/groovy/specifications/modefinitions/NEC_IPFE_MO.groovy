package specifications.modefinitions

import java.util.concurrent.atomic.AtomicInteger
import static specifications.utils.MetricDictionary.*


class NEC_IPFE_MO extends MONITORED_OBJECT {

	NEC_IPFE_MO(String id, String uuid,String name){
        mo_type = 'nec-ipfe'
        expected_metrics = ["9149468465651731743","9148777916851236947","9149468464151731705",
                            "9149468486251732731","9148777928151239648","9149468470951732213",
                            "9148778345151886279","9148778346451886520","9148778348551887021",
                            "9148778350151887279","9148778349351887174","9148777906251234947",
                            "9148777909351235381","9148777911151235672","9147377400951462451"]
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