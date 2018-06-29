package specifications.utils

class FileDownloader {
	
	def metric_download_part_1 = "curl -k -u superuser:Superpassword_123 http://mano-global-dtag.deploy22.openshift.sdntest.netcracker.com/dt/monitoring/v1/metrics/files/"
	def metric_download_part_2 = "/export  -s -o "
	
	def getFile(String filename, String downloadDir){
		def metric_file_download_command = metric_download_part_1 + filename + metric_download_part_2 + downloadDir + "/" + filename
		metric_file_download_command.execute()
	}
		
}