pipeline {

    agent any
    environment {
        ANSIBLE_HOST_KEY_CHECKING = 'false'
    	VERSION = "0.1"
        //AWS_REGION = "us-east-1"
        //AWS_CREDENTIAL = "aws-credentials-jenkins-s3"
        //S3_ARTIFACT = "semperti-rapientrega-development-s3-backend-artifact"
        //S3_ARTIFACT_OBJECT = "rapientrega.zip"
    }
    stages {
        stage('Stage 1 - Checkout') {
            steps {
                echo "STAGE1 - Checkout"
                sh "ls -ltr"
                //deleteDir()
                checkout scm
            }
        }
        stage('Stage 2 - Unit Test') {
            steps {
                echo "STAGE2 - Unit Test"
                sh "pwd ; ls -ltr"
                sh "mvn clean "
                sh "mvn test"
            }
        }
        stage('Stage 3 - Release & Package') {
            steps {
                echo "STAGE3 - Release & Upload Nexus"
                sh "mvn versions:set -DnewVersion=$env.VERSION"
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Stage 4 - Create Artifact') {
            steps {
                echo "STAGE 4 - Create Artifact"
                sh "echo test > serco.crt"
                zip archive: true, glob: 'Dockerfile, serco.crt, target/*.jar', zipFile: 'rapientrega.zip', overwrite: true
            }
        }
        stage('Stage 5 - Upload Artifact to S3 Bucket') {
            steps {
                echo "STAGE 5 - Upload Artifact to S3 Bucket"
                withAWS(credentials: "aws-credentials-jenkins-s3", region: "us-east-1") {
                    s3Upload(file:"rapientrega.zip", 
                    bucket:"semperti-rapientrega-development-s3-backend-artifact", 
                    path:"rapientrega.zip")
                }
            }
        }
        stage('Stage 6 - Check CodePipeline Running') {
            steps {
                echo "STAGE 6 - Check CodePipeline Running"
                // Wait to manual approve
                timeout(300) {
                    waitUntil {
                        script {
                            def r = sh script: 'curl -s http://development.eba-fkx55m2f.us-east-1.elasticbeanstalk.com/message', returnStatus: true
                            return (r == 0);
                        }
                    }
                }
            }
        }
        stage('Stage 7 - Check Application RUN') {
            steps {
                echo 'STAGE 7 - Check Application RUN'
                steps {
                    sh "curl -s http://development.eba-fkx55m2f.us-east-1.elasticbeanstalk.com/message"
                }
            }
        }
    }
}
