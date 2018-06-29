package specifications.utils

import java.util.concurrent.atomic.AtomicInteger

class MetricDictionary {

         private static final Map<String,List> unique_metric_base = [

                 "9146523577551079863":["Ping Success Rate (management-net)",5],
                 "9149468465651731743":["PPP Sessions",1],
                 "9148777916851236947":["Real Memory Available",1],
                 "9149468464151731705":["Switched Octets",1],
                 "9149468486251732731":["Switched Packets",1],
                 "9148777928151239648":["System Processes Current",1],
                 "9149468470951732213":["VRF Entries",1],
                 "9148778345151886279":["CPU Group Usage (all)",5],
                 "9148778346451886520":["CPU Group Usage (manager)",5],
                 "9148778348551887021":["CPU Group Usage (u-plane-rx)",5],
                 "9148778350151887279":["CPU Group Usage (u-plane-tx)",5],
                 "9148778349351887174":["CPU Group Usage (u-plane-worker)",5],
                 "9148777906251234947":["Load Average, 15min",5],
                 "9148777909351235381":["Load Average, 1min",5],
                 "9148777911151235672":["Load Average, 5min",5],
                 "9147377400951462451":["Operational Status",5],
                 "9149468490251733105":["NAT Translations",1],
                 "9146886307051157430":["Buffer Pool Usage (FPC: FPC @ 0/*/*)",1],
                 "9146886313251157553":["Buffer Pool Usage (midplane)",1],
                 "9146886318151157647":["Buffer Pool Usage (PIC: VSRX DPDK GE @ 0/0/*)",1],
                 "9146886324051157780":["Buffer Pool Usage (Routing Engine)",1],
                 "9147377659851475509":["LSYS SP Zones Used",1],
                 "9146886335151157909":["CPU Usage (FPC: FPC @ 0/*/*)",5],
                 "9146886341951158008":["CPU Usage (midplane)",5],
                 "9146886348151158130":["CPU Usage (PIC: VSRX DPDK GE @ 0/0/*)",5],
                 "9146886353551158240":["CPU Usage (Routing Engine)",5],
                 "9149464936551562662":["Load Average, 15min (FPC: FPC @ 0/*/*)",5],
                 "9149464938751562826":["Load Average, 15min (midplane)",5],
                 "9149464941151563340":["Load Average, 15min (PIC: VSRX DPDK GE @ 0/0/*)",5],
                 "9149464944251563590":["Load Average, 15min (Routing Engine)",5],
                 "9146886361651158362":["Load Average, 1min (FPC: FPC @ 0/*/*)",5],
                 "9146886367551158469":["Load Average, 1min (midplane)",5],
                 "9146886577951159727":["Load Average, 1min (PIC: VSRX DPDK GE @ 0/0/*)",5],
                 "9146886587751159928":["Load Average, 1min (Routing Engine)",5],
                 "9146886592451160040":["Load Average, 5min (FPC: FPC @ 0/*/*)",5],
                 "9146886723651977428":["Load Average, 5min (midplane)",5],
                 "9146886734351977679":["Load Average, 5min (PIC: VSRX DPDK GE @ 0/0/*)",5],
                 "9146886741051977812":["Load Average, 5min (Routing Engine)",5],
                 "9146886747351977917":["Operating State (FPC: FPC @ 0/*/*)",5],
                 "9146886756751978074":["Operating State (midplane)",5],
                 "9146886763551978183":["Operating State (PIC: VSRX DPDK GE @ 0/0/*)",5],
                 "9146886769851978451":["Operating State (Routing Engine)",5],
                 "9148995037251908423":["DHCP Leases",1],
                 "9148995033151907627":["DHCP Subscribers",1],
                 "9148994923851888944":["Access Accepts",1],
                 "9148994929951890589":["Access Challenges",1],
                 "9148994933551891118":["Access Rejects",1],
                 "9148994937951892221":["Access Requests",1],
                 "9148994941051892623":["Accounting Dropped Requests",1],
                 "9148994944251893284":["Accounting Duplicate Requests",1],
                 "9148994948751894048":["Accounting Invalid Requests",1],
                 "9148994954151895218":["Accounting Malformed Requests",1],
                 "9148994957351895705":["Accounting Requests",1],
                 "9148994960551896535":["Accounting Responses",1],
                 "9148994964951897206":["Accounting Unknown Types",1],
                 "9148994968451897875":["Auth Dropped Requests",1],
                 "9148994971451898355":["Auth Duplicate Requests",1],
                 "9148994975151898917":["Auth Invalid Requests",1],
                 "9148994978051899293":["Auth Malformed Requests",1],
                 "9148994981551899849":["Auth Unknown Types",1],
                 "9149460807051379057":["Operational Status",5]
         ]

        static Integer getExpectedSampleCountByMetricId(String metric_id){ (Integer) unique_metric_base.find{it.key == metric_id}.value[1]}

        static String getExpectedMetricName(String metric_id){ (String) unique_metric_base.find {it.key == metric_id}.value[0]}

        static Map<String, List> getMetricDetails(List<String> metric_ids ){ unique_metric_base.findAll {it.key in metric_ids} }
}



