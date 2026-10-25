# Authentication, JWT, OAuth 2.0 and OpenID Connect

Authentication establishes identity. Authorization decides what that identity may do. JWT is a token representation; OAuth 2.0 is a delegated authorization framework; OpenID Connect adds an identity layer.

## Implementation sequence
1. Configure a trusted OIDC provider.
2. Register client and API audience.
3. Configure Spring Security as an OAuth2 resource server.
4. Validate signature, issuer, expiry, and audience as required.
5. Map trusted scopes/claims to authorities.
6. Enforce object-level authorization: customers can only access their own orders.
7. Use client credentials for service-to-service access where appropriate.
8. Test missing, malformed, expired, wrong-issuer, wrong-audience, and insufficient-scope tokens.

## Security rules
- Never trust a token merely because it can be Base64-decoded.
- Keep access tokens short-lived; protect and rotate refresh tokens.
- Never log bearer tokens or secrets.
- A valid token is not a substitute for service-level authorization.
- Prefer Authorization Code + PKCE for public clients; never embed client secrets in mobile/browser code.

## Lab
Change an order ID in the URL to another customer's order. The service must reject the request unless the principal owns the order or has explicit permission.
