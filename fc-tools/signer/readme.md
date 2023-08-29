# Federated Catalog Signer

## Usage

To sign and validate SDs a private-public key pair is required.
To create a self-signed pair this command can be executed:

```
openssl req -x509 -newkey rsa:4096 -keyout prk.ss.pem -out cert.ss.pem -sha256 -days 365 -nodes
```


Afterwards the SDs can be signed and verified. To do this build the project as part of the overall FC project, then run the signer tool for concrete SD file:

```
java -jar fc-tools-signer-<project.version>-full.jar <path_to_original_SD_file> <path_to_signed_SD_file>
```


