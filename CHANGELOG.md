# v0.3.2

* Fixed handling of refresh tokens.
* Fixed a typo in TokenResponse.
* Use native GPG functions in GitHub actions.

# v0.3.1

* Library updates.
* Added multithreaded option to mvn in `Makefile`.

# v0.3.0

* Added GraphConnectionHandler. A basic connection to the HIRO Graph without any authentication. Can be used as root
  connection for several TokenAPIHandlers, i.e. a FixedTokenApiHandler with a token for each user that shall all use the
  same connection.
* Added CookieManager to connections.

# v0.2.2

* Try to get token via requestToken() if refreshToken() fails with any HiroHttpException.

# v0.2.1

* Handle invalid refresh tokens and retry via getToken().

# v0.2.0

* Add method to decode HIRO token data.
* Documentation, fixes and refactorings.

# v0.1.2

Make API classes extendable

# v0.1.1

Deployment fixes

# v0.1.0

Initial release
