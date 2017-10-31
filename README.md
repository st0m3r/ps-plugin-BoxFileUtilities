# ps-plugin-BoxFileUtilities
Utilities to integrate Appian with Box

Comprised of 4 parts:
Create JWT Token
File Upload Smart Service
File Download to Appian KC/Folder
Smart Service
Custom Function
File Download Servlet
Create JWT Token
Provides a custom function called “createtoken” that will provide the user with a JWT that can be used for up to 60 seconds as authentication with the /token API from Box.  The function can take 8 parameters; however, only two are required.
Parameters (See Header and Claims above for descriptions):
sub
Required
box_sub_type
Required
alg
typ
kid
iss
aud
jti
Examples:
WIthin an Interface or Expression Rule:
rule!Box(
jwt: fn!createtoken(
sub: cons!BOX_SERVICE_USER,
customClaims: "user"
),
onSuccess: {
a!save(
ri!accessCode,
a!fromJson(fv!result.body).access_token
)
},
onError: {
a!save(
ri!accessCode,
fv!result
)
}
)

In this example, rule!Box is an Integration to the https://api.box.com/oauth2/token Box API which receives the JWT created by the plugin and returns an access token valid for 60 seconds for use with Box APIs.  The BOX_SERVICE_USER constant is set to 2338120597.  The managed user I created using the APIs has an ID of 2644852656 and I created a constant for that as well (BOX_APP_USER).

Within a Process Model:

The circled nodes are used to create the JWT, call the /token API and parse the received token.

The endpoint is to the /token api, method is POST and Body is shown above which contains the Box App client_id, client_secret (these should be constants) and a call to the createtoken function.  The response is stored in a process variable “responseBody” and parsed in a script task:

This access_token is stored and then used in any API calls.

File Upload Smart Service
The upload smart service is shown in the example process model above.  After obtaining a JWT, this smart service uses the access token and calls the file upload API passing in the contents of a provided Appian Document.

The service takes three inputs: Document ID, File Name and Token.
File Download to Appian
This part of the plugin exposes both a custom function as well as a smart service to download a file from Box and store it in a provided Appian Folder.
Example custom function:
if(
not(isnull(local!accessCode)),
a!save(
local!fileInfo,
fn!downloadDocumentToAppian(
document: local!fileId,
folder: local!folderId,
token: local!accessCode
)
),
{}
)
In the above example, a File ID and Folder ID are stored in local variables via Interface component saves.
Example Smart Service:


The smart service inputs are: Document ID, Folder and Token.
File Download Servlet
The servlet is very similar to the function and smart service, however, does not create an Appian Document, instead it downloads the file directly to the browser for the user to view/save to their computer.  The servlet takes two parameters:
document - the Box ID of the document to download
token - the access code obtained with a JWT

Example Usage:
a!safeLink(
label: ri!axDocId,
uri: concat(
rule!APN_getSiteUrl(),
"plugins/servlet/boxfiledownload?document=",
ri!documentID,
"&token=",
ri!accessCode
)
)
