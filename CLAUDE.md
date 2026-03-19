# Claude Code Guidelines for octopus-sdk-android

## Slack Support Workflow

When handling requests from Slack, follow this minimal-notification approach:

### Response Protocol
1. **Acknowledge silently** - Start working immediately without posting "I'm on it" messages
2. **Work without updates** - No intermediate status messages during implementation
3. **Single completion message** - Post one concise summary when done:

```
Done: [brief description]
PR: [link] | Status: [Open/Merged]
```

### Example Format
```
Done: Fixed broken SSO documentation links
PR: https://github.com/Octopus-Community/octopus-sdk-android/pull/2 | Status: Merged
```

### Guidelines
- Keep messages under 3 lines when possible
- Include PR link and current status
- No emoji unless specifically requested
- Reply in thread, don't notify channel
