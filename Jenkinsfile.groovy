// Set your project Prefix using your GUID
// Set variable globally to be available in all stages
def prefix      = "semperti"
def app = "rapientrega"

// Set Maven command to always include Nexus Settings
//def mvnCmd      = "mvn -s ./nexus_settings.xml"
def mvnCmd      = "mvn"

// Set Development and Production Project Names
def devEnv  = "${prefix}-${app}-dev"
def prodEnv = "${prefix}-${app}-prod"

def prodEnvBlue  = "${prefix}-${app}-blue"
def prodEnvGreen = "${prefix}-${app}-green"

// Set the tag for the development image: version + build number
def devTag      = "0.0-0"
// Set the tag for the production image: version
def prodTag     = "0.0"
def destApp     = "rapientrega"
def activeApp   = ""

// Artifact
def dev = "development"
def prod = "production"
def artifactDev = "${app}.zip"
def artifactProd = "${app}.zip"
def s3ArtifactDev = "${prefix}-${app}-${dev}-s3-backend-artifact"
def s3ArtifactProd = "${prefix}-${app}-${prod}-s3-backend-artifact"
//def awsCredentials = "aws-credentials-jenkins-s3"
def awsCredentials = "aws-credentials-semperti"
def awsRegion = "us-east-1"

// ElasticBeanstalk Blue/Green
def ebDevAppBlue = "rapientrega-eb"
def ebDevEnvBlue = "development"
def ebDevEnvGreen = "development-green"
def ebUrlDevBlue = "development.eba-fkx55m2f.us-east-1.elasticbeanstalk.com"
def ebUrlDevGreen = "development.eba-fkx55m2f.us-east-1.elasticbeanstalk.com"
def ebUrlDevBluePath = "message"
def ebUrlDevGreenPath = "message"

//def ebProdApp = ""
//def ebProdEnv = ""

pipeline {

    agent any
    //environment {
    //    ANSIBLE_HOST_KEY_CHECKING = 'false'
    //}
    stages {
        stage('Checkout Source') {
            steps {
                // git credentialsId: 'dc69dd47-d601-4cb0-adbe-548c17e15506', url: "http://<gitRepo>/<username>/<repoName>.git"
                checkout scm
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version
                    devTag  = "${version}-" + currentBuild.number
                    prodTag = "${version}"
                }
            }
        }
        stage('Build War File') {
            steps {
                echo "Building version ${devTag}"
                sh "${mvnCmd} versions:set -DnewVersion=$env.${devTag}"
                sh "${mvnCmd} clean package -DskipTests=true"
            }
        }
        stage('Unit Tests') {
            steps {
                echo "Running Unit Tests"
                sh "${mvnCmd} test"
                // It displays the results of tests in the Jenkins Overview
                //step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
            }
        }
        stage('Code Analysis') {
            steps {
                script {
                echo "Running Code Analysis"
                //sh "${mvnCmd} sonar:sonar -Dsonar.host.url=http://sonarqube:9000/ -Dsonar.projectName=${JOB_BASE_NAME} -Dsonar.projectVersion=${devTag}"
                }
            }
        }
        stage('Publish to Nexus') {
            steps {
                echo "Publish to Nexus"
                //sh "${mvnCmd} deploy -DskipTests=true -DaltDeploymentRepository=nexus::default::http://nexus:8081/repository/releases"
            }
        }
        stage('Create Zip Artifact') {
            steps {
                echo "STAGE 4 - Create Artifact"
                //Test cert, delete this line after the probe.
                sh "echo test > serco.crt"
                zip archive: true, glob: 'Dockerfile, serco.crt, target/*.jar', zipFile: 'rapientrega.zip', overwrite: true
            }
        }
        stage('Upload Artifact to S3 Bucket') {
            steps {
                input "Upload Artifact ${artifactDev} to S3 Bucket ${s3ArtifactDev}?"
                println artifactDev
                println s3ArtifactDev
                withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {
                    s3Upload(file:"${artifactDev}", bucket:"${s3ArtifactDev}", path:"${artifactDev}")
                }
            }
        }
        stage('Check CodePipeline Deploy ElasticBeanstalk') {
            steps {
                echo "Check CodePipeline Running"
                //withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {
                //    ebWaitOnEnvironmentHealth(
                //        applicationName: "${ebDevAppBlue}", 
                //        environmentName: "${ebDevEnvBlue}",
                //        health: "Green",
                //        stabilityThreshold: 60
                //    )
                //}
                script {
                    def ebDeployStatus = sh(script: """aws codepipeline list-action-executions --pipeline-name semperti-rapientrega-development-pipeline-backend | jq '.actionExecutionDetails[] | select(.status=="Succeeded" and .stageName=="Deploy") | .status'""", returnStdout: true).trim()
                    println("Deploy Status = ${ebDeployStatus}")
                    //while (ebDeployStatus != "Succeeded") {
                    //    sleep 5
                    //    echo "Waiting for ${ebDevAppBlue} to be ready"
                    //}
                }
            }
        }
        stage('Check Application is Up and Running') {
            steps {
                echo "Check CodePipeline Running"
                timeout(300) {
                    waitUntil {
                        script {
                            def r = sh script: "curl -s http://${ebUrlDevBlue}/${ebUrlDevBluePath}", returnStatus: true
                            return (r == 0);
                        }
                    }
                }
            }
        }
    }
}
