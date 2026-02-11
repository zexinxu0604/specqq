# SpecQQ Chatbot Router - User Operations Guide

**Version**: 1.0.0 | **Last Updated**: 2026-02-11

This guide provides step-by-step instructions for administrators to use the SpecQQ Chatbot Router web console effectively.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Logging In](#logging-in)
3. [Dashboard Overview](#dashboard-overview)
4. [Managing Message Rules](#managing-message-rules)
5. [Using CQ Code Patterns](#using-cq-code-patterns)
6. [Managing Group Chats](#managing-group-chats)
7. [Viewing Message Logs](#viewing-message-logs)
8. [Understanding Statistics](#understanding-statistics)
9. [Testing Rules](#testing-rules)
10. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites

- Web browser (Chrome, Firefox, Safari, or Edge - latest version recommended)
- Network access to the SpecQQ server
- Administrator credentials

### Accessing the Web Console

1. Open your web browser
2. Navigate to: `http://your-server-address:5173` (development) or your production URL
3. You should see the login page

---

## Logging In

### First-Time Setup

If this is your first time accessing the system:

1. **Navigate to the login page**
2. **Use default credentials**:
   - Username: `admin`
   - Password: `admin123`
3. Click **"Login"** button
4. **⚠️ IMPORTANT**: Change the default password immediately after first login for security

### Regular Login

1. Enter your **username** in the "Username" field
2. Enter your **password** in the "Password" field
3. Click **"Login"** button
4. If credentials are correct, you'll be redirected to the dashboard

### Session Management

- Sessions expire after **24 hours** of inactivity
- You'll be automatically logged out when the session expires
- Click **"Logout"** in the top-right corner to manually end your session

---

## Dashboard Overview

After logging in, you'll see the main dashboard with the following sections:

### Navigation Menu (Left Sidebar)

- **Dashboard**: Overview and quick statistics
- **Rules Management**: Create and manage message routing rules
- **Group Chats**: Manage QQ group configurations
- **Message Logs**: View message history and activity
- **Statistics**: View detailed analytics

### Top Bar

- **User Info**: Displays your username (e.g., "admin")
- **Logout Button**: Click to end your session

### Main Content Area

The main area displays content based on your current navigation selection.

---

## Managing Message Rules

Message rules define how the chatbot responds to incoming messages. Rules are evaluated by priority, and the first matching rule determines the response.

### Viewing Rules

1. Click **"Rules Management"** in the left sidebar
2. You'll see a table with all existing rules showing:
   - **Rule Name**: Descriptive name for the rule
   - **Match Type**: How the rule matches messages (Keyword, Regex, Exact, Prefix, Suffix)
   - **Match Pattern**: The pattern used for matching
   - **Reply Content**: What the bot will reply
   - **Priority**: Execution order (lower number = higher priority)
   - **Status**: Enabled/Disabled
   - **Actions**: Edit, Delete, Toggle buttons

### Creating a New Rule

1. Click the **"Create Rule"** button (top-right)
2. Fill in the rule form:

   **Basic Information**:
   - **Rule Name**: Enter a descriptive name (e.g., "Welcome Message")
   - **Description**: Optional detailed description
   - **Priority**: Set execution order (1 = highest priority, lower numbers execute first)

   **Matching Configuration**:
   - **Match Type**: Select from dropdown:
     - **Keyword**: Matches if message contains the keyword anywhere
     - **Regex**: Matches using regular expression pattern
     - **Exact**: Matches only if message exactly equals the pattern
     - **Prefix**: Matches if message starts with the pattern
     - **Suffix**: Matches if message ends with the pattern

   - **Match Pattern**: Enter the pattern to match
     - For Keyword: `hello` (matches "hello", "say hello", "hello world")
     - For Exact: `hello` (matches only "hello")
     - For Prefix: `!command` (matches "!command", "!command args")
     - For Suffix: `?` (matches "how are you?", "what?")
     - For Regex: `\d{6}` (matches 6-digit numbers)

   **Response Configuration**:
   - **Reply Type**: Select response type:
     - **Text**: Plain text response
     - **Image**: Send an image (provide image URL)
     - **Forward**: Forward to another group

   - **Reply Content**: Enter the response message
     - Use `{user}` to insert the sender's name
     - Use `{group}` to insert the group name
     - Example: "Hello {user}! Welcome to {group}!"

   **Advanced Settings**:
   - **Enable Rule**: Toggle to enable/disable immediately
   - **Rate Limiting**: Prevent spam (5-second cooldown, max 3 triggers per user)

3. Click **"Save"** to create the rule
4. The rule will appear in the rules table

### Editing an Existing Rule

1. Find the rule in the rules table
2. Click the **"Edit"** button (pencil icon)
3. Modify any fields in the form
4. Click **"Save"** to apply changes
5. Click **"Cancel"** to discard changes

### Deleting a Rule

1. Find the rule in the rules table
2. Click the **"Delete"** button (trash icon)
3. Confirm deletion in the popup dialog
4. The rule will be permanently removed

⚠️ **Warning**: Deletion cannot be undone. Export your configuration before deleting important rules.

### Enabling/Disabling Rules

You can temporarily disable rules without deleting them:

1. Find the rule in the rules table
2. Click the **"Toggle"** switch in the Status column
3. **Green** = Enabled (rule is active)
4. **Gray** = Disabled (rule is inactive)

Disabled rules remain in the system but won't match any messages.

### Rule Priority

Rules are evaluated in **priority order** (lowest number first):

- **Priority 1**: Evaluated first
- **Priority 2**: Evaluated second (if Priority 1 doesn't match)
- **Priority 10**: Evaluated after all lower-priority rules

**Best Practices**:
- Assign priority 1-10 to critical rules (exact matches, admin commands)
- Assign priority 50-100 to general rules (keywords, common responses)
- Assign priority 500+ to catch-all rules (fallback responses)

### Filtering and Searching

Use the search and filter controls above the rules table:

- **Search by Name**: Enter rule name in the search box
- **Filter by Status**: Select "Enabled" or "Disabled" from dropdown
- **Filter by Match Type**: Select match type from dropdown
- **Reset Filters**: Click "Reset" to clear all filters

---

## Using CQ Code Patterns

CQ codes are special message formats used by QQ to represent rich content like emojis, images, and mentions. The system provides predefined patterns for easy rule configuration.

### What are CQ Codes?

CQ codes look like this: `[CQ:face,id=123]` or `[CQ:image,file=abc.jpg]`

Common types:
- **Face**: Emoji/stickers `[CQ:face,id=123]`
- **Image**: Pictures `[CQ:image,file=abc.jpg]`
- **At**: Mentions `[CQ:at,qq=123456]`
- **Reply**: Quote replies `[CQ:reply,id=123]`
- **Record**: Voice messages `[CQ:record,file=abc.amr]`
- **Video**: Video messages `[CQ:video,file=abc.mp4]`

### Using the CQ Code Pattern Selector

When creating or editing a rule with **Match Type = Regex**:

1. You'll see a **"CQ Code Pattern"** section
2. Click the dropdown to open the pattern selector
3. **Step 1**: Select CQ code type (Face, Image, At, etc.)
4. **Step 2**: Select specific pattern from the list
5. The regex pattern will **auto-fill** into the "Match Pattern" field
6. A preview card shows:
   - Pattern description
   - Example matches
   - Parameter filters (if available)

### Pattern Examples

**Example 1: Match any face emoji**
- Type: Face
- Pattern: "匹配任意表情" (Match any face)
- Generated regex: `\[CQ:face,id=(\d+)\]`
- Matches: `[CQ:face,id=123]`, `[CQ:face,id=456]`

**Example 2: Match specific user mention**
- Type: At
- Pattern: "匹配特定用户@" (Match specific user @)
- Generated regex: `\[CQ:at,qq=123456\]`
- Matches: Only `[CQ:at,qq=123456]`

**Example 3: Match any image**
- Type: Image
- Pattern: "匹配任意图片" (Match any image)
- Generated regex: `\[CQ:image,file=[^,\]]+\]`
- Matches: `[CQ:image,file=abc.jpg]`, `[CQ:image,file=xyz.png]`

### Parameter Filters

Some patterns support parameter filters to narrow matching:

1. After selecting a pattern, you'll see filter inputs
2. Enter specific values to match (e.g., QQ number for @mentions)
3. The regex pattern updates automatically
4. Leave blank to match any value

### Testing CQ Code Patterns

Use the **Live Preview** feature:

1. Select a CQ code pattern
2. In the preview card, enter a test message
3. The system shows whether the message matches
4. Matched CQ codes are highlighted with tags
5. Adjust the pattern if needed

---

## Managing Group Chats

Group chats represent QQ groups where the bot is active. You must configure groups before rules can apply to them.

### Viewing Groups

1. Click **"Group Chats"** in the left sidebar
2. You'll see a table with all configured groups:
   - **Group ID**: QQ group identifier (numeric)
   - **Group Name**: Friendly name for the group
   - **Status**: Enabled/Disabled
   - **Rule Count**: Number of rules bound to this group
   - **Actions**: Edit, View Rules, Toggle buttons

### Adding a New Group

1. Click the **"Add Group"** button (top-right)
2. Fill in the group form:
   - **Group ID**: Enter the QQ group number (e.g., `123456789`)
   - **Group Name**: Enter a friendly name (e.g., "Tech Support Group")
   - **Enable Group**: Toggle to enable immediately
3. Click **"Save"** to add the group

**How to find Group ID**:
- In QQ desktop client: Right-click group → Group Info → Group ID
- In QQ mobile app: Group settings → Group Info → Group ID

### Binding Rules to Groups

Rules must be bound to groups to take effect:

1. Find the group in the groups table
2. Click **"View Rules"** button
3. You'll see currently bound rules
4. Click **"Bind New Rule"** button
5. Select rules from the available rules list
6. Click **"Bind"** to apply

**Multiple Rules per Group**:
- A group can have multiple rules
- Rules are evaluated by priority
- First matching rule determines the response

**Multiple Groups per Rule**:
- A rule can be bound to multiple groups
- Same rule behavior applies to all bound groups

### Unbinding Rules from Groups

1. Navigate to the group's rules view
2. Find the rule to unbind
3. Click the **"Unbind"** button (unlink icon)
4. Confirm unbinding in the popup
5. The rule is removed from this group (but not deleted globally)

### Enabling/Disabling Groups

Temporarily disable a group without deleting it:

1. Find the group in the groups table
2. Click the **"Toggle"** switch in the Status column
3. **Green** = Enabled (bot active in this group)
4. **Gray** = Disabled (bot ignores messages from this group)

---

## Viewing Message Logs

Message logs show all messages processed by the chatbot, including matches and responses.

### Accessing Message Logs

1. Click **"Message Logs"** in the left sidebar
2. You'll see a table with recent messages:
   - **Timestamp**: When the message was received
   - **Group Name**: Which group sent the message
   - **Sender**: QQ number of the sender
   - **Message Content**: The original message
   - **Matched Rule**: Which rule matched (if any)
   - **Reply Content**: What the bot replied
   - **Status**: Success/Failed

### Filtering Logs

Use filters to find specific messages:

- **Date Range**: Select start and end dates
- **Group Filter**: Select specific group from dropdown
- **Rule Filter**: Select specific rule from dropdown
- **Status Filter**: Success, Failed, or All
- **Search**: Enter text to search in message content

Click **"Apply Filters"** to update the view.

### Log Details

Click on any log entry to view full details:

- **Message ID**: Unique identifier
- **Timestamp**: Exact date and time
- **Group Info**: Group ID and name
- **Sender Info**: QQ number and nickname
- **Original Message**: Full message text with CQ codes
- **Matched Rule**: Rule name and match pattern
- **Reply Sent**: Full reply content
- **Processing Time**: How long matching took (milliseconds)
- **Error Info**: Error message if processing failed

### Exporting Logs

Export logs for external analysis:

1. Apply desired filters
2. Click **"Export"** button (top-right)
3. Select format:
   - **CSV**: For Excel/spreadsheet analysis
   - **JSON**: For programmatic processing
4. The file downloads to your browser

### Log Retention

- Logs are retained for **90 days** by default
- Older logs are automatically archived
- Contact your administrator to retrieve archived logs

---

## Understanding Statistics

The Statistics page provides insights into message patterns and bot performance.

### Accessing Statistics

1. Click **"Statistics"** in the left sidebar
2. Select a time range (Today, Last 7 Days, Last 30 Days, Custom)
3. View various charts and metrics

### Key Metrics

**Message Volume**:
- **Total Messages**: Number of messages received
- **Matched Messages**: Messages that matched a rule
- **Unmatched Messages**: Messages with no matching rule
- **Match Rate**: Percentage of matched messages

**CQ Code Statistics**:
- **Total CQ Codes**: Count of CQ codes in messages
- **By Type**: Breakdown by CQ code type (Face, Image, At, etc.)
- **Top Patterns**: Most common CQ code patterns
- **Formatted Display**: Chinese labels with icons (e.g., "表情×10")

**Rule Performance**:
- **Most Triggered Rules**: Rules with highest match count
- **Average Response Time**: How fast rules process
- **Error Rate**: Percentage of failed rule executions

**Group Activity**:
- **Messages by Group**: Which groups are most active
- **Peak Hours**: When messages are most frequent
- **Active Users**: Most active senders

### Reading Charts

**Line Chart (Message Trends)**:
- X-axis: Time (hourly or daily)
- Y-axis: Message count
- Hover over points to see exact values

**Pie Chart (CQ Code Distribution)**:
- Each slice represents a CQ code type
- Hover to see count and percentage
- Click legend to show/hide types

**Bar Chart (Rule Performance)**:
- Bars show match count per rule
- Sorted by frequency (highest first)
- Hover to see exact numbers

### Interpreting Statistics

**High Match Rate (>80%)**: Good coverage, most messages handled
**Low Match Rate (<50%)**: Consider adding more rules or adjusting patterns

**Unbalanced CQ Codes**: If one type dominates (e.g., 90% Face), optimize rules for that type

**Slow Response Times (>100ms)**: Check regex complexity or database performance

---

## Testing Rules

Test rules before deploying them to ensure they work correctly.

### Using the Rule Tester

1. Navigate to **Rules Management**
2. Click **"Test Rule"** button (top-right)
3. Fill in the test form:
   - **Select Rule**: Choose a rule to test
   - **Test Message**: Enter a sample message
   - **Group Context**: Select which group to simulate (optional)
4. Click **"Run Test"**
5. View test results:
   - **Match Result**: Whether the rule matched
   - **Extracted Data**: Captured groups from regex (if applicable)
   - **Reply Preview**: What the bot would reply
   - **Processing Time**: How long matching took

### Test Scenarios

**Scenario 1: Keyword Matching**
- Rule: Match Type = Keyword, Pattern = "help"
- Test Message: "I need help with something"
- Expected: ✅ Match (contains "help")

**Scenario 2: Exact Matching**
- Rule: Match Type = Exact, Pattern = "hello"
- Test Message: "hello world"
- Expected: ❌ No Match (not exact)

**Scenario 3: Regex with CQ Codes**
- Rule: Match Type = Regex, Pattern = `\[CQ:face,id=(\d+)\]`
- Test Message: "Hello [CQ:face,id=123] there"
- Expected: ✅ Match, Captured: "123"

### Debugging Failed Tests

If a test doesn't match as expected:

1. **Check Match Type**: Ensure it's appropriate for your pattern
2. **Verify Pattern Syntax**: Test regex patterns at regex101.com
3. **Check Case Sensitivity**: Keywords are case-insensitive by default
4. **Escape Special Characters**: In regex, escape `.`, `*`, `+`, `?`, `[`, `]`, etc.
5. **Test Incrementally**: Start with simple patterns, then add complexity

---

## Troubleshooting

### Common Issues

#### Issue 1: Rule Not Matching Messages

**Symptoms**: Messages arrive but no reply is sent

**Possible Causes**:
1. Rule is disabled
2. Rule priority is too low (another rule matched first)
3. Match pattern is incorrect
4. Group is not bound to the rule
5. Group is disabled

**Solutions**:
1. Check rule status (should be green/enabled)
2. Lower the rule priority to test (set to 1 temporarily)
3. Use the Rule Tester to validate the pattern
4. Verify group binding in Group Chats → View Rules
5. Check group status (should be green/enabled)

#### Issue 2: Bot Replies Too Slowly

**Symptoms**: Replies arrive with noticeable delay (>5 seconds)

**Possible Causes**:
1. Complex regex patterns
2. Too many rules (slow priority scanning)
3. Database performance issues
4. Network latency

**Solutions**:
1. Simplify regex patterns (avoid nested groups, excessive backtracking)
2. Reduce total rule count or consolidate similar rules
3. Check database connection in logs
4. Contact administrator for server health check

#### Issue 3: Cannot Login

**Symptoms**: Login fails with error message

**Possible Causes**:
1. Incorrect username or password
2. Session expired
3. Server connection issue

**Solutions**:
1. Verify credentials (check for typos, caps lock)
2. Clear browser cache and cookies
3. Try a different browser
4. Contact administrator to reset password

#### Issue 4: CQ Code Patterns Not Working

**Symptoms**: CQ code rules don't match messages with CQ codes

**Possible Causes**:
1. Wrong match type (must be Regex)
2. Pattern not properly escaped
3. CQ code format changed

**Solutions**:
1. Ensure Match Type is set to "Regex"
2. Use the CQ Code Pattern Selector instead of manual entry
3. Test with the Live Preview feature
4. Check NapCat documentation for current CQ code format

#### Issue 5: Rate Limiting Triggered

**Symptoms**: Bot stops replying after several rapid messages, shows "Rate limit exceeded" in logs

**Explanation**: This is a security feature, not a bug. Rate limiting prevents spam and abuse.

**Default Limits**:
- **Cooldown**: 5 seconds between rule triggers
- **Max Triggers**: 3 per user within the cooldown window

**Solutions**:
1. Wait 5 seconds before sending another message
2. If legitimate high-frequency use case, contact administrator to adjust limits
3. Check for message loops (bot replying to itself)

---

## Best Practices

### Rule Organization

1. **Use Clear Names**: "Welcome Message" not "Rule 1"
2. **Add Descriptions**: Document the purpose of each rule
3. **Logical Priorities**: Group related rules with similar priorities
4. **Regular Cleanup**: Remove unused or outdated rules

### Pattern Design

1. **Start Simple**: Use Keyword/Exact before Regex
2. **Test Thoroughly**: Use the Rule Tester before deployment
3. **Avoid Greedy Regex**: Prefer specific patterns over `.*`
4. **Use CQ Code Selector**: Don't write CQ code regex manually

### Group Management

1. **Descriptive Names**: Use clear group names, not just IDs
2. **Selective Binding**: Only bind relevant rules to each group
3. **Monitor Activity**: Check logs regularly for each group
4. **Disable Unused Groups**: Keep the active group list clean

### Performance Optimization

1. **Prioritize Exact Matches**: Put exact matches at high priority (faster than regex)
2. **Limit Regex Complexity**: Avoid nested groups and backtracking
3. **Consolidate Rules**: Combine similar rules when possible
4. **Regular Audits**: Review and optimize rules quarterly

---

## Keyboard Shortcuts

- **Ctrl/Cmd + K**: Open search
- **Ctrl/Cmd + S**: Save current form (when editing)
- **Esc**: Close modal dialogs
- **Tab**: Navigate between form fields

---

## Getting Help

### Support Channels

- **Documentation**: Check this guide and API documentation
- **Issue Tracker**: Report bugs at https://github.com/your-org/specqq/issues
- **Administrator**: Contact your system administrator for access issues

### Providing Feedback

When reporting issues, include:
1. What you were trying to do
2. What you expected to happen
3. What actually happened
4. Screenshots (if applicable)
5. Browser and version
6. Timestamp of the issue

---

## Glossary

- **CQ Code**: QQ-specific message format for rich content (emojis, images, mentions)
- **Match Type**: Method used to compare messages against patterns (Keyword, Regex, Exact, etc.)
- **Priority**: Execution order of rules (lower number = higher priority)
- **Rate Limiting**: Security feature that limits how often rules can trigger
- **Rule Binding**: Associating a rule with one or more groups
- **JWT**: JSON Web Token, used for authentication

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-11
**Feedback**: Please report issues or suggestions to your administrator
