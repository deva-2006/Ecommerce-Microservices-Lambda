import { Amplify } from "aws-amplify";

Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: "us-east-1_uAT4POYI0",
      userPoolClientId: "48gv27bhkv88o27nr4omf30rh8",
      region: "us-east-1"
    }
  }
});

// Snyk trigger

