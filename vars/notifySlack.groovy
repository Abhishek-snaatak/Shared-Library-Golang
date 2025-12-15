def call(Map config = [:]) {

  def status = config.status ?: 'UNKNOWN'
  def slackCredId = config.slackCredId ?: 'slack-webhook'

  def colorEmoji = status == 'SUCCESS' ? '✅' : '❌'
  def buildUrl = env.BUILD_URL
  def jobName = env.JOB_NAME
  def buildNumber = env.BUILD_NUMBER
  def timestamp = sh(script: "date '+%Y-%m-%d %H:%M:%S'", returnStdout: true).trim()

  withCredentials([string(credentialsId: slackCredId, variable: 'SLACK_WEBHOOK_URL')]) {
    sh """
      payload='{
        "blocks": [
          {
            "type": "header",
            "text": { "type": "plain_text", "text": "${colorEmoji} BUILD ${status}" }
          },
          {
            "type": "section",
            "fields": [
              { "type": "mrkdwn", "text": "*Job:*\\n${jobName}" },
              { "type": "mrkdwn", "text": "*Build #:*\\n#${buildNumber}" },
              { "type": "mrkdwn", "text": "*Triggered By:*\\nJenkins" },
              { "type": "mrkdwn", "text": "*Status:*\\n${status}" }
            ]
          },
          {
            "type": "section",
            "text": {
              "type": "mrkdwn",
              "text": "*Build URL:*\\n<${buildUrl}|Open in Jenkins>"
            }
          },
          {
            "type": "context",
            "elements": [
              { "type": "mrkdwn", "text": "🕒 *Time (IST):* ${timestamp}" }
            ]
          }
        ]
      }'

      curl -s -X POST -H "Content-type: application/json" \
        --data "\$payload" "\$SLACK_WEBHOOK_URL"
    """
  }
}
