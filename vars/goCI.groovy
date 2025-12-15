def call(Map config = [:]) {

  def gitUrl       = config.gitUrl ?: error("gitUrl is required")
  def gitBranch    = config.gitBranch ?: 'main'
  def slackCredId  = config.slackCredId ?: 'slack-webhook'

  def binaryName
  def buildPath = '.'
  def debugLog  = "${env.WORKSPACE}/compile_debug.log"

  try {

    stage('Checkout') {
      echo "Checking out ${gitUrl} @ ${gitBranch}"
      git url: gitUrl, branch: gitBranch
    }

    stage('Detect Go Build Path') {
      sh """
        set -e
        if [ -d cmd ]; then
          echo "cmd directory detected"
          echo "cmd" > .build_path
        else
          echo "root build detected"
          echo "." > .build_path
        fi
      """

      buildPath = readFile('.build_path').trim()
      binaryName = gitUrl.tokenize('/').last().replace('.git', '')

      echo "Build Path: ${buildPath}"
      echo "Binary Name: ${binaryName}"
    }

    stage('Verify Go Environment') {
      sh """
        set -xe
        echo ">>> Verifying Go installation" | tee -a ${debugLog}
        go version | tee -a ${debugLog}
        go env GOPATH GOROOT | tee -a ${debugLog}
      """
    }

    stage('Clean & Compile') {
      sh """
        set -xe
        rm -f ${binaryName}

        echo ">>> Downloading dependencies" | tee -a ${debugLog}
        go mod download | tee -a ${debugLog}

        echo ">>> Building Go binary" | tee -a ${debugLog}
        CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
          go build -o ${binaryName} ${buildPath} | tee -a ${debugLog}
      """
    }

    stage('Verify Compilation Output') {
      sh """
        set -xe
        if [ ! -f ${binaryName} ]; then
          echo "ERROR: Binary not found!" | tee -a ${debugLog}
          exit 20
        fi
        ls -lh ${binaryName}
      """
    }

    stage('Archive Artifacts') {
      archiveArtifacts artifacts: "${binaryName}", fingerprint: true
      archiveArtifacts artifacts: "compile_debug.log", fingerprint: true
    }

    notifySlack(
      status: 'SUCCESS',
      slackCredId: slackCredId
    )

  } catch (err) {

    notifySlack(
      status: 'FAILED',
      slackCredId: slackCredId
    )

    throw err
  }
}
