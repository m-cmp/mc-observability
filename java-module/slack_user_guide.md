# Slack Bot Setup Guide

This guide outlines the steps required to create, configure, and set up a Slack bot 
to receive mc-o11y events and alarms within a Slack workspace.

## Prerequisites

- A Slack account with access to a workspace.
- Appropriate permissions to create apps and channels in the workspace.

## Step 1: Create a Workspace (If Necessary)

1. Navigate to [https://slack.com/get-started](https://slack.com/get-started) to create a new Slack workspace, if you haven't already.

## Step 2: Create a New Slack App

1. Visit [https://api.slack.com/apps](https://api.slack.com/apps).
2. Click **Create New App**.  
![img.png](images/img.png)
3. Choose **From scratch**.  
![img.png](images/img2.png)
4. Provide a name for your app and select the workspace where it will be used.  
![img.png](images/img3.png)

## Step 3: Configure OAuth & Permissions

1. Go to **OAuth & Permissions** in your app's settings. (OAuth & Permissions > Scopes > Add an OAuth Scope)
2. Under the **Scopes** > **Bot Token Scopes** section, click **Add an OAuth Scope**.  
![img.png](images/img4.png)
3. Ensure that the scope `chat:write` is included, as this is required to send messages from the bot.

## Step 4: Install the App in Slack
![img.png](images/img7.png)
1. In the app settings, navigate to the **Install App** section. (OAuth & Permissions > OAuth Tokens)
2. Click **Install** (or **Reinstall**, if applicable).
3. After installation, review and approve the required permissions.
4. Upon successful installation, return to the **OAuth & Permissions** page and copy the **Bot User OAuth Token** for later use.
![img.png](images/img5.png)

## Step 5: Create and Configure a Slack Channel

1. Return to the slack workspace page.  
2. Create the channel where your bot will operate (e.g., `trigger-bot`).
3. Right-click the app you created > Click 'View App Details' > Click 'Add this app to a channel' > Select the channel to receive the alarm > Add the app you created
4. After completing the above course, you are ready to use the mc-o11y event/alarm function.
![img.png](images/img6.png)

## Step 6: Use Token

1. After that, when using the mc-o11y event/alarm slack user registration API(POST /api/o11y/trigger/policy/{policySeq}/alert/slack), you can enter the pre-generated **Bot User OAuth token**.