# Octopus SDK Android Sample - SSO Hybrid Profile

Some profile fields are managed by your app. This means these fields are the ones
that will be used in the community. It also means that Octopus Community won't moderate those
fields (nickname, bio, or picture profile). It also means that, if the nickname is part of
these fields, you have to ensure that it is unique.

## In order to test this scenario:

- your community should be configured to use SSO authentication
- your community should be configured to have some app managed fields
- you must set those fields in the OctopusSDK.initialize() function