# Octopus SDK Android Sample - SSO Client Profile

All profile fields are managed by your app. This means that your user profile is the one that
will be used in the community. It also means that Octopus Community won't moderate the content
of the profile (nickname, bio, and picture profile). It also means that you have to ensure
that the nickname is unique.

## In order to test this scenario:

- your community should be configured to use SSO authentication
- your community should be configured to have all fields as app managed
- you must set those fields in the OctopusSDK.initialize() function