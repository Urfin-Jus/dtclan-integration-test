package specifications

import groovy.json.JsonOutput
import specifications.modefinitions.*
import specifications.utils.DataProvider
import spock.lang.*
import io.restassured.response.Response

import java.util.concurrent.atomic.AtomicInteger

import static specifications.utils.Alfred.*
import io.restassured.specification.RequestSpecification

class API_MetricFileContent_Spec extends Specification{

    @Shared String file_name = 'raw-metrics-20180510055400-20180510055900.csv'
    @Shared RequestSpecification requestSpec
    @Shared def file_content

    //INDEPENDANT VALIDATION COUNTERS
    @Shared Integer row                         // processed rows count
    @Shared Integer empty_rows                  // list of empty rows faced
    @Shared Integer uncategorized_type
    @Shared List uncategorized_objects

    /////////RUN TIME ENCOUNTERS
    //mo-id vs monitoried object
    @Shared Map<String,Object> encountered_mo

    //mo-type vs encountered monitored object count
    @Shared Map<String,AtomicInteger> object_counters

    def setupSpec(){
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        encountered_mo = [:]
        object_counters = [
                "nec-ipfe": new AtomicInteger(),
                "nec-nat":  new AtomicInteger(),
                "nec-sfc-gw":new AtomicInteger(),
                "juniper-vsrx":new AtomicInteger(),
                "nec-dhcp":new AtomicInteger(),
                "freeradius":new AtomicInteger(),
                "freeradius-database":new AtomicInteger(),
                "nec-cwp-business-logic":new AtomicInteger(),
                "nec-cwp-database":new AtomicInteger(),
                "nec-cwp-haproxy":new AtomicInteger(),
                "nec-cwp-redis":new AtomicInteger(),
                "vm":new AtomicInteger()
        ]
        uncategorized_objects = []
        uncategorized_type = 0
        empty_rows = 0
        row = 1
        file_content = (Response) getMetricFile(requestSpec,file_name)
            assert file_content != ""
        println "METRIC FILE RECEIVED. CONTENT IS NOT EMPTY"
            parseCSVfile(file_content.asString())

        def objectParsingResults = """MONITORED OBJECTS: ${encountered_mo.size()}
         ENCOUNTERED MONITORED OBJECTS PER TYPE:
        ${JsonOutput.prettyPrint(JsonOutput.toJson(object_counters))}"""
        objectParsingResults.readLines().each { reportHeader(it)}


        //First parsing results
        println objectParsingResults

        //Run Validation per every parsed object
        encountered_mo.values().each { it.validate() }
    }

    def "There is no line with empty required values"(){
        expect:'There is onle one empty row possible at every end'
            if (empty_rows != 0){
                uncategorized_objects.each {println it.mo_id}
            }
            assert empty_rows <= 1

    }

    def "There is no uncategorized MO types"(){
        expect: 'There is no objects, which were parsed with unknown type'
        if (uncategorized_type != 0){
            uncategorized_objects.each {println it.mo_id}
        }
        assert uncategorized_type == 0

        //encountered_mo.values().each{ println it.actual_sample_encounters}
    }

    def "Total sample number of all objects is equal to processed rows in file"(){
       setup: "Total sample count parsed per each file, together is equal to processed file row count"
        Integer total_parsed_sample_count = 0
        encountered_mo.values().each{ total_parsed_sample_count += it.calculateTotalMetricCount() }

        expect:
         assert total_parsed_sample_count == row

    }

    @Unroll("Validate metric samples per Monitored Object: #mo_type | #mo_id | #mo_name ")
    def "Validate metric samples per Monitored Object"(){
        given: 'Take parsed monitored object as start point and form a report'
         mo.getReport().readLines().each { reportInfo(it)}

        expect: 'Check if Total Sample Count / Expected Sample Count per Metric / Unrelated Metric Count are expected values '
            assert ( mo.isActualTotalCount_Passed() && mo.isAllExpectedSamplesRecieved() && mo.noUnrelatedMetricSamples() )

        where:
            mo << new DataProvider(object_list: encountered_mo.values()).iterator() //.setObjectList(encountered_mo)
            mo_type = mo.mo_type
            mo_name = mo.mo_name
            mo_id   = mo.mo_id
    }


    def parseCSVfile(String file_content) {
        // [0] mo_object_id,[1] uuid(fqdn), [2] mo_object_type, [3] mo_object_name, [4] metric_id, [5] metric_name, [6] metric_value, [7] timestamp
        file_content.splitEachLine(';') { line ->
            //println "$row ROW | " + line
            if ((line == null) || (line[0] == null) || (line[2] == null) || (line[4] == null)) {
                empty_rows++
                //println "!!!!EMPTY STRING!!!! ROW NUM = $row | " +  line
                row++
            } else {
                if (!encountered_mo.keySet().contains(line[0])) {

                    //Store new parsed object
                        encountered_mo.put(line[0], createMObyType(line))

                    //Increment MO statistics
                        incrementObjectStats(line[2])

                    //Increment Metric statistics of NEW MO
                        //Check if metric is expected and then increment counter
                        if ( getMOActualSampleEncounters(line[0]).find {it.key == line[4] } ) {
                             getMOActualSampleEncounters(line[0]).find {it.key == line[4] }.value.incrementAndGet()}
                        else {
                            preserveUnrelatedObjectMetric(line[0],line[4],line[5])
                        }
                        //println "$row ROW | NEW OBJECT IS CREATED | ID: ${line[0]}| TYPE: ${line[2]} | First Metric ID: ${line[4]} "
                        row++
                } else {
                    //Increment Metric statistics of EXISTING MO
                        //Check if metric is expected and then increment counter
                        if ( getMOActualSampleEncounters(line[0]).find {it.key == line[4] } ) { getMOActualSampleEncounters(line[0]).find {it.key == line[4] }.value.incrementAndGet()}
                        else {
                        preserveUnrelatedObjectMetric(line[0],line[4],line[5])
                        //assert getMOActualSampleEncounters(line[0]).find {it.key == line[4] }.value != null
                        }
                        //println "$row ROW | EXISTING OBJECT IS UPDATED:   ${line[0]}| TYPE: ${line[2]} | Metric ID: ${line[4]} | Actual Samples : " + JsonOutput.prettyPrint(JsonOutput.toJson(getMOActualSampleEncounters(line[0])))
                        row++
                }
            }
        }
    }

    def createMObyType(List<String> line){
        switch(line[2]){
            case 'nec-ipfe'     : new NEC_IPFE_MO(line[0], line[1],line[3])         ; break
            case 'nec-nat'      : new NEC_NAT_MO(line[0], line[1],line[3])          ; break
            case 'nec-sfc-gw'   : new NEC_SFC_GW_MO(line[0], line[1],line[3])       ; break
            case 'juniper-vsrx' : new JUNIPER_VSRX_MO(line[0], line[1],line[3])     ; break
            case 'nec-dhcp'     : new NEC_DHCP_MO(line[0], line[1],line[3])         ; break
            case 'freeradius'   : new FREERADIUS_MO(line[0], line[1],line[3])       ; break
            case 'vm'           : new VM_MO(line[0], line[1],line[3])               ; break
            case ["freeradius-database","nec-cwp-business-logic","nec-cwp-database", "nec-cwp-haproxy", "nec-cwp-redis"] : new GENERIC_MO(line[0], line[1], line[2],line[3]) ; break
            default: uncategorized_type++; uncategorized_objects << line[0] ; break
        }

    }

    def incrementObjectStats(String mo_type){ object_counters.find { it.key == mo_type }.value.incrementAndGet() }

    def preserveUnrelatedObjectMetric(String mo_id, String metric_id, String metric_name){
        getObjectActualUnrelatedSampleEncounters(mo_id).put( metric_id , new AtomicInteger() )
        getUnrelatedMetrics(mo_id).put( metric_id, metric_name )
        ((AtomicInteger) getObjectActualUnrelatedSampleEncounters(mo_id).find { it.key == metric_id}.value).incrementAndGet()
    }

    Map<String, AtomicInteger> getMOActualSampleEncounters(String mo_id){ (Map<String,AtomicInteger>) encountered_mo.find { it.key == mo_id }.value['actual_sample_encounters'] }

    Map<String, AtomicInteger> getObjectActualUnrelatedSampleEncounters(String mo_id){ ( Map<String, AtomicInteger> ) encountered_mo.find{it.key == mo_id}.value['actual_unrelated_encounters'] }

    Map<String,String> getUnrelatedMetrics(String mo_id){ (Map<String,String>) encountered_mo.find { it.key == mo_id }.value['unrelated_metrics']}
}
