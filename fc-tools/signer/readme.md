# Federated Catalog Signer

## Usage

To sign and validate SDs a private-public key pair is required.
To create a self-signed pair this command can be executed:

`openssl req -x509 -newkey rsa:4096 -keyout prk.ss.pem -out cert.ss.pem -sha256 -days 365 -nodes`

Afterwards the SDs can be signed and verified.
To do so the paths in `Main.java` might have to be changed. 
Finally, by running the code one can create a signed SD. 
The location can be specified here: `PATH_TO_SIGNED_SELF_DESCRIPTION`
