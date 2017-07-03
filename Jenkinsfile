properties([
  pipelineTriggers([
    upstream(
      threshold: hudson.model.Result.SUCCESS,
      upstreamProjects: '/metaborg/spoofax-releng/master' // build this project after Spoofax-master is built
    )
  ]),
  buildDiscarder(logRotator(artifactNumToKeepStr: '3')),
  disableConcurrentBuilds() //disableds parallel builds
])

node{
  try{
    notifyBuild('Started')

    stage('Checkout') {
      checkout scm
      sh "git clean -fXd"
    }

    stage('Build and Test') {
      withMaven(
        //mavenLocalRepo: "${env.JENKINS_HOME}/m2repos/${env.EXECUTOR_NUMBER}", //http://yellowgrass.org/issue/SpoofaxWithCore/173
        mavenLocalRepo: ".repository",
        mavenOpts: '-Xmx1G -Xms1G -Xss16m'
      ){
        sh 'mvn -B -U clean verify -DforceContextQualifier=\$(date +%Y%m%d%H%M)'
      }
    }

    stage('Archive') {
      archiveArtifacts(
        artifacts: 'java.eclipse.site/target/site/',
        excludes: null,
        onlyIfSuccessful: true
      )
    }

    stage('Cleanup') {
      sh "git clean -fXd"
    }

    notifyBuild('Succeeded')

  } catch (e) {

    notifyBuild('Failed')
    throw e

  }
}

// def notifyBuild(String buildStatus) {
//  def message = """${buildStatus}: ${env.JOB_NAME} [${env.BUILD_NUMBER}] ${env.BUILD_URL}"""

//  if (buildStatus == 'Succeeded') {
//    color = 'good'
//  } else if (buildStatus == 'Failed') {
//    color = 'danger'
//  } else {
//    color = '#4183C4' // Slack blue
//  }

//  slackSend (color: color, message: message, channel: '#some-slack-channel')
//}
