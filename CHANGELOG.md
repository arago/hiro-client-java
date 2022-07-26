# v0.5.1

Library update. Use arago java-project 0.4.2.

# v0.5.0

Refactorings

* Made `AbstractClientAPIHandler` into `DefaultHttpClientHandler` without references to `AbstractAPIHandler`.
* Created interfaces HttpClientHandler and TokenAPIHandler.
* Got rid of the copy constructors.
* Added and modified documentation.
* Do not ignore `FixedTokenException` when a token is invalid and cannot be changed, but add it to the exception chain.


# v0.4.0

* Code Auth and Org switch.
* Improved integration tests.
* Event-ws handling debugged (query and fragment were missing before).

# v0.3.5

* Changes for compatibility with auth api 6.6 of HIRO graph.

# v0.3.4

* Send 401 when token is unauthorized and cannot be refreshed. Avoid being hidden by a FixedTokenException.

# v0.3.3

* Bugfixed URI handling. Old handling removed query parameters.

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
