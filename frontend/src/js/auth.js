import "./amplify-config.js";
import { signUp, confirmSignUp, signIn, signOut, fetchAuthSession, fetchUserAttributes as _fetchUserAttributes, resendSignUpCode, signInWithRedirect, updatePassword } from "aws-amplify/auth";

export { _fetchUserAttributes as fetchUserAttributes };

export async function doChangePassword(oldPassword, newPassword) {
  try {
    await updatePassword({ oldPassword, newPassword });
    return { success: true };
  } catch (err) {
    console.error("Change password error:", err);
    return { success: false, error: err.message };
  }
}

// ---- OAUTH / SOCIAL SIGN IN (Google, Facebook, Apple) ----
export async function doSocialSignIn(provider = "Google") {
  try {
    await signInWithRedirect({ provider });
  } catch (err) {
    console.error("Social Sign In Error:", err);
    return { success: false, error: err.message };
  }
}

let _cachedAttrs = null;

// ---- GET USER ATTRIBUTES (cached per session, avoids repeat Lambda calls) ----
export async function getCachedUserAttributes() {
  if (_cachedAttrs) return _cachedAttrs;
  _cachedAttrs = await _fetchUserAttributes();
  return _cachedAttrs;
}

export function clearAttributeCache() {
  _cachedAttrs = null;
}

// ---- SIGN UP ----
export async function doSignUp(email, password) {
  try {
    await signUp({
      username: email,
      password,
      options: { userAttributes: { email } }
    });
    return { success: true };
  } catch (err) {
    console.error("SignUp error:", err);
    return { success: false, error: err.message, code: err.name };
  }
}

// ---- CONFIRM SIGN UP ----
export async function doConfirmSignUp(email, code) {
  try {
    await confirmSignUp({ username: email, confirmationCode: code });
    return { success: true };
  } catch (err) {
    console.error("Confirm error:", err);
    return { success: false, error: err.message };
  }
}

// ---- RESEND SIGN UP CODE ----
export async function doResendSignUp(email) {
  try {
    await resendSignUpCode({ username: email });
    return { success: true };
  } catch (err) {
    console.error("ResendSignUp error:", err);
    return { success: false, error: err.message };
  }
}

// ---- SIGN IN ----
export async function doSignIn(email, password) {
  try {
    // clear any lingering session first (Amplify blocks signIn if one exists)
    try {
      await signOut();
    } catch (e) {
      // no active session — ignore
    }
    await signIn({ username: email, password });
    return { success: true };
  } catch (err) {
    console.error("SignIn error:", err);
    return { success: false, error: err.message, code: err.name };
  }
}

// ---- SIGN OUT ----
export async function doSignOut() {
  await signOut();
  clearAttributeCache();
}

// ---- GET ACCESS TOKEN (use as Bearer token on backend calls) ----
export async function getAccessToken() {
  try {
    const session = await fetchAuthSession();
    return session.tokens?.idToken?.toString() ?? session.tokens?.accessToken?.toString() ?? null;
  } catch (err) {
    return null;
  }
}

// ---- GUARD: redirect to login if no active session ----
export async function requireAuth() {
  const token = await getAccessToken();
  if (!token) {
    window.location.href = "/login.html";
  }
  return token;
}

// ---- GET USER GROUPS (from ID token claim) ----
export async function getUserGroups() {
  try {
    const session = await fetchAuthSession();
    return session.tokens?.idToken?.payload?.["cognito:groups"] ?? [];
  } catch (err) {
    return [];
  }
}

export async function isAdmin() {
  const groups = await getUserGroups();
  return groups.includes("Admin");
}

// ---- GUARD: redirect to admin-login if not an Admin ----
export async function requireAdmin() {
  const token = await getAccessToken();
  if (!token) {
    window.location.href = "/login.html";
    return false;
  }
  const admin = await isAdmin();
  if (!admin) {
    window.location.href = "/home.html";
    return false;
  }
  return true;
}
