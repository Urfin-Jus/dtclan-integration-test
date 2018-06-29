package specifications

import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import spock.lang.*
import static io.restassured.RestAssured.*
import static specifications.utils.Alfred.*
import static specifications.utils.Properties.network_service_endpoint_Primary
import static specifications.utils.Properties.network_service_endpoint_Secondary


class API_Health_Check_Spec extends Specification {


    @Shared RequestSpecification requestSpec
    @Shared branch_service_instance = 'API-INVENTORY-06190136-BRANCH-INSTANCE-06190136'
    @Shared String access_token

    @Shared List<Map> active_primary_cpe_branches
    @Shared List<Map> active_secondary_cpe_branches

    def setupSpec(){
        setPollingDefaults()
        requestSpec = setupRequestSpec(setBasicAuthScheme())
        access_token = getToken()
    }

    //@Unroll("Get Primary Network Service List: #ns_endpoint")
    def "Get Primary Network Service List"(){
        setup:
            def cpe_domain  ; def vas_domain  ; def cpe_primary; def cpe_secondary
            def dis_cpe_domain  ; def dis_vas_domain  ; def dis_cpe_primary; def dis_cpe_secondary
            Response ns_instance = getNSlist(access_token,ns_endpoint)
            List<Object> active_ns_branches = ns_instance.jsonPath().getList('$').findAll {it.status == 'ACTIVE'}
            List<Object> disconnected_ns_branches = ns_instance.jsonPath().getList('$').findAll {it.status == 'DISCONNECTED'}

            //Active only
            cpe_domain = active_ns_branches.findAll{ it['key'].toString().contains('cpe-domain')}
            vas_domain = active_ns_branches.findAll{ it['key'].toString().contains('vas-domain')}
            cpe_primary = active_ns_branches.findAll{ it['key'].toString().contains('cpe-branch')}
            cpe_secondary = active_ns_branches.findAll{ it['key'].toString().contains('cpe-secondary')}

            active_primary_cpe_branches = cpe_primary

            //Disconnected only
            dis_cpe_domain = disconnected_ns_branches.findAll{ it['key'].toString().contains('cpe-domain')}
            dis_vas_domain = disconnected_ns_branches.findAll{ it['key'].toString().contains('vas-domain')}
            dis_cpe_primary = disconnected_ns_branches.findAll{ it['key'].toString().contains('cpe-branch')}
            dis_cpe_secondary = disconnected_ns_branches.findAll{ it['key'].toString().contains('cpe-secondary')}

            println '#############################################'
            println '# ' + ns_endpoint

            println '###############- ACTIVE -####################'

            println "Total Active Services: " + active_ns_branches.size() //toString()
            println "CPE Domain Services: " + cpe_domain.size()
            println "VAS Domain Services: " + vas_domain.size()
            println "CPE Branch Services: " + cpe_primary.size()
            println "CPE Secondary Branch Services: " + cpe_secondary.size()

            println '#############- DISCONNECTED -################'
            println "Total Disconnected Services: " + disconnected_ns_branches.size() //toString()
            println "CPE Domain Services: " + dis_cpe_domain.size()
            println "VAS Domain Services: " + dis_vas_domain.size()
            println "CPE Branch Services: " + dis_cpe_primary.size()
            println "CPE Secondary Branch Services: " + dis_cpe_secondary.size()


        expect:
            cpe_domain.size() + vas_domain.size() + cpe_primary.size() + cpe_secondary.size() == active_ns_branches.size()

        and:
            dis_cpe_domain.size() + dis_vas_domain.size() + dis_cpe_primary.size() + dis_cpe_secondary.size() == disconnected_ns_branches.size()

        where:

        ns_endpoint << [network_service_endpoint_Primary, network_service_endpoint_Secondary]

    }


    def "Check if Endpoint is alive"(){
        setup:
        //active_primary_cpe_branches.collect{it.}
        given().spec(requestSpec).get("/om/v1/serviceInstance/$branch_service_instance/healthcheck")

    }






}
