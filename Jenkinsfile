pipeline {
    agent any

    // Poll the repo every 5 min for changes
    triggers { pollSCM('H/5 * * * *') }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare') {
            steps {
                // Gradle wrapper must be executable on Unix
                sh 'chmod +x gradlew'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew --no-daemon clean compileJava'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew --no-daemon test'
            }
            post {
                always {
                    // Publish test results even if some fail
                    junit 'app/build/test-results/test/*.xml'
                }
            }
        }

        // Sprint 2 (Henry, Scrum Master): run Checkstyle in CI and keep its reports.
        stage('Code Quality') {
            steps {
                sh './gradlew --no-daemon checkstyleMain checkstyleTest'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'app/build/reports/checkstyle/**',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Coverage') {
            steps {
                sh './gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification'
            }
            post {
                always {
                    // Archive the HTML report so tutors can download it
                    archiveArtifacts artifacts: 'app/build/reports/jacoco/**',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                // Sprint 2 (Henry): also build the runnable distribution (bin/app + libs)
                // so every green build has an offline executable for the client.
                sh './gradlew --no-daemon jar distZip'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'app/build/libs/*.jar, app/build/distributions/*.zip',
                                     allowEmptyArchive: true
                }
            }
        }
    }

    post {
        success {
            echo 'Build passed. Tests, coverage, and JAR all generated.'
        }
        failure {
            echo 'Build failed — check Console Output above.'
        }
    }
}
