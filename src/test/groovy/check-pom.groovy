import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory

class MavenProjectModelCheck {
	File baseDir

	static String stripSlash(String input) {
		if (input.startsWith('/')) {
			input = input.substring(1)
		}
		if (input.endsWith('/')) {
			input = input.substring(0, input.length() - 1)
		}
		return input
	}

	int processPom(File folder) {
		baseDir = folder
		def pomFile = new File(folder, 'pom.xml')
		return checkPom(pomFile)
	}

	int checkPom(File pomFile) {
		if (!pomFile.exists()) {
			throw new RuntimeException("POM not found ${pomFile}")
		}
		def moduleName = pomFile.parentFile.name
		def relative = stripSlash(new File(pomFile.parentFile.path - baseDir.path, pomFile.name).path)
		println("INFO checking $relative")
		def xmlDoc = DOMBuilder.parse(new FileReader(pomFile))
		def project = xmlDoc.documentElement
		def count = 0
		use(DOMCategory) {

			def missing = []

			def name = project.getElementsByTagName('name') // .name is swallowed by Node.name()
			if (!name && name.length == 0) {
				missing.add("name")
			}
			if (!project.description?.text()) {
				missing.add("description")
			}
			if (!project.parent?.version?.text()) {
				if (!project.url.text()) {
					missing.add("url")
				}
				if (!project.organization?.text()) {
					missing.add("organization")
				}
				if (!project.licenses?.text()) {
					missing.add("licenses")
				}
				if (!project.scm?.text()) {
					missing.add("scm")
				}
				if (!project.developers?.text()) {
					missing.add("developers")
				}
			}
			if (missing.size() > 0) {
				System.err.println("ERROR: Missing tags:" + moduleName + ":$missing")
			}
			count = missing.size();
			if (project.modules) {
				for (def module : project.modules.module) {
					def childName = module.text()
					if (childName.length() > 0) {
						count += checkPom(new File(pomFile.parentFile, "$childName/pom.xml"))
					}
				}
			}
		}
		return count
	}

}

def mavenPom = new MavenProjectModelCheck()
def missing = mavenPom.processPom(new File(properties ? properties['basedir'] ?: '.' : '.'))
if (missing > 0) {
	System.exit(1)
}
