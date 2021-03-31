// Set variable globally to be available in all stages

//def mvnCmd      = "mvn -s ./nexus_settings.xml"
def mvnCmd      = "mvn"

// Set the tag for the development image: version + build number
def devTag      = "0.0-0"

// Set the tag for the production image: version
def prodTag     = "0.0"

// Artifact
def prefix = "semperti"
def app = "rapientrega"
def env = "development"
def artifact = "${app}.zip"
def s3Artifact = "${prefix}-${app}-${env}-s3-backend-artifact"

// AWS
//def awsCredentials = "aws-credentials-jenkins-s3"
def awsCredentials = "aws-credentials-semperti"
def awsRegion = "us-east-1"
def awsProfile = "semperti"

// CodePipeline
def codepipelineName = "${prefix}-${app}-${env}-pipeline-backend"
// ElasticBeanstalk
def ebApp = "${app}-eb"
def ebEnv = "${env}"
def ebUrl = "development.eba-fkx55m2f.us-east-1.elasticbeanstalk.com"
def ebUrlPath = "message"

//def ebAppBlue = "${app}-eb-blue"
//def ebEnvBlue = "${env}-blue"
//def ebUrlBlue = "development-blue.eba-fkx55m2f.us-east-1.elasticbeanstalk.com"
//def ebUrlBlue = "message"

//def ebAppGreen = "${app}-eb-greenlue"
//def ebEnvGreen = "${env}-green"
//def ebUrlGreen = "development-green.eba-fkx55m2f.us-east-1.elasticbeanstalk.com"
//def ebUrlGreen = "message"


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
                input "Upload Artifact ${artifact} to S3 Bucket ${s3Artifact}?"
                println artifact
                println s3Artifact
                withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {
                    s3Upload(file:"${artifact}", bucket:"${s3Artifact}", path:"${artifact}")
                }
            }
        }
        stage('Check CodePipeline Deploy ElasticBeanstalk') {
            steps {
                echo "Check CodePipeline Running"
                //Bug Reported: https://plugins.jenkins.io/pipeline-aws/
                //withAWS(credentials: "${awsCredentials}", region: "${awsRegion}") {
                //    ebWaitOnEnvironmentHealth(
                //        applicationName: "${ebApp}", 
                //        environmentName: "${ebEnv}",
                //        health: "Green",
                //        stabilityThreshold: 60
                //    )
                //}
                script {
                    def pipelineStatus = ""
                    def pipelineId = ""
                    echo "Waiting for CodePipeline InProgress State...."

                    while (pipelineStatus != "InProgress" ) {
                    
                        def pipelineStatus = sh(script: """aws codepipeline get-pipeline-state --name semperti-rapientrega-development-pipeline-backend | jq '.stageStates[1].latestExecution.status'""", returnStdout: true).trim()
                        def pipelineId = sh(script: """aws codepipeline get-pipeline-state --name semperti-rapientrega-development-pipeline-backend | jq '.stageStates[1].latestExecution.pipelineExecutionId'""", returnStdout: true).trim()
                    
                        echo "CodePipeline: ${codepipelineName} with ID: ${pipelineId} in Status: ${pipelineStatus}"
                        sleep 10
                    }
                    
                    echo "Codepipeline ID: ${pipelineId} is in ${pipelineStatus} Status, waiting for Succeeded..."
                    
                    while (pipelineStatus != "Succeeded") {
                    
                        def pipelineStatus = sh(script: """aws codepipeline get-pipeline-state --name semperti-rapientrega-development-pipeline-backend | jq '.stageStates[1].latestExecution.status'""", returnStdout: true).trim()
                    
                        echo "Codepipeline ID: ${pipelineId} is in ${pipelineStatus} Status, waiting for Succeeded..."
                        sleep 10
                    }
                    
                    echo "Codepipeline ID: ${pipelineId} is in ${pipelineStatus} Status, the Artifact is deployed"
                }
            }
        }
        stage('Check Application is Up and Running') {
            steps {
                echo "Check CodePipeline Running"
                timeout(300) {
                    waitUntil {
                        script {
                            def r = sh script: "curl -s http://${ebUrl}/${ebUrlPath}", returnStatus: true
                            return (r == 0);
                        }
                    }
                }
            }
        }
    }
}
