pipeline {

    agent any
    environment {
        ANSIBLE_HOST_KEY_CHECKING = 'false'
    	VERSION = "0.1"
    }
    stages {
        stage('Stage 1 - Configure & Clean Slave') {
            steps {
                echo "STAGE1 - Tasks pre Test and build"
                //sh "git clone https://github.com/gonzaloacosta/springboot-apirest app"
                sh "ls -ltr"
            }
        }
        stage('Stage 2 - Unit Test') {
            steps {
                echo "STAGE2 - Unit Test"
                sh "pwd ; ls -ltr"
                sh "mvn test"
            }
        }
        stage('Stage 3 - Release & Upload Nexus') {
            steps {
                echo "STAGE3 - Release & Upload Nexus"
                sh "mvn versions:set -DnewVersion=$env.VERSION"
                sh "mvn clean package -DskipTests"
            }
        }
        stage('Stage 4 - Snapshot & Upload Nexus') {
            steps {
                echo "STAGE 4 - Snapshot & Upload Nexus"
                sh "mvn versions:set -DnewVersion=$env.VERSION-SNAPSHOT"
                sh "mvn clean package -DskipTests" 
            }
        }
        stage('Stage 5 - Docker build, tag & push images ') {
            steps {
                echo "STAGE 5 - Docker build, tag & push images"
                //withCredentials([usernamePassword(credentialsId: 'ga-docker-hub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {	
                //dir("${env.WORKSPACE}/ansible"){
                //    sh "ansible-playbook stage5-docker-build.yml --extra-vars @vars/ansible-vars.json -e VERSION=$env.VERSION -e USERNAME=$USERNAME -e PASSWORD=$PASSWORD"
                //}
            } 
        }
        stage('Stage 6 - Docker pull & run') {
            steps {
                echo "STAGE 6 - Docker pull & run"
                //dir("${env.WORKSPACE}/ansible"){
                //    sh "pwd"
                //    sh "ansible-playbook --extra-vars @vars/ansible-vars.json stage6-docker-run.yml -e VERSION=$env.VERSION"
                //}
                //timeout(300) {
                //    waitUntil {
                //        script {
                //            def r = sh script: 'curl http://localhost:8080', returnStatus: true
                //            return (r == 0);
                //        }
                //    }
                //} 
            }
        }
        stage('Stage 7 - Check Application RUN') {
            steps {
                echo 'STAGE 7 - Check Application RUN'
                //steps {
                //    sh "curl http://localhost:8080"
                //}
            }
        }
    }
}
