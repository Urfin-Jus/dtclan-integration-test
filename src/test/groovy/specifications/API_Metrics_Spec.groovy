package specifications

import groovy.json.JsonSlurper
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

import static specifications.utils.Alfred.*

import spock.lang.*
import groovy.time.TimeCategory

class API_Metric_File_List_Spec extends Specification {
    @Shared Response listResponse
    @Shared def listFromResponse = []
    @Shared def listSize = 0
    @Shared List<Date> start_time_list = [] as List
    @Shared List<Date>   end_time_list = [] as List
    @Shared RequestSpecification requestSpec

    def setupSpec(){
        requestSpec = setupRequestSpec(setBasicAuthScheme())

    }


    //@Unroll('list_1 size check against #expectedResult')
    def "Get Metric List"(){

        when: 'list of currently available metric files is requested'
        listResponse =  getMetricList(requestSpec)
        listFromResponse = new JsonSlurper().parseText( listResponse.asString())
        listSize = listFromResponse.size()

        then: 'list of metric file names is returned, not null-sized & does not extend 100 count'
        assert (listSize > 0) && (listSize <= 100)

    }

    def "Check every single file name timestamps of collacting period"(){

        when: 'take start & end timestamps from each file name and check time interval'
        listFromResponse = listFromResponse.sort()
        listFromResponse.each{
            def name_split = it.toString().tokenize('.')[0].tokenize('-')
            start_time_list << new Date().parse ('yyyyMMddHHmmss',name_split[2])
            end_time_list << new Date().parse ('yyyyMMddHHmmss',name_split[3])
        }


        then: 'time interval is equal to 5 mins'
        (0..<listSize).each {

            assert ( new TimeCategory().minus(end_time_list[it],start_time_list[it]).minutes ) == 5

        }

    }

    def "Check if files cover timeline evenly, meaning there are no gaps in metric file generation"(){
        expect: 'Time diff between neighbor metric files after sorting is 5 mins'
        (1..<listSize).each {
            assert ( new TimeCategory().minus(start_time_list[it],start_time_list[(it-1)]).minutes ) == 5
        }

    }


}


