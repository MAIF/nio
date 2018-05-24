# API

**Nio** features are provided through it's API.

## Swagger

Swagger for API documentation may be found [here](https://raw.githubusercontent.com/MAIF/nio/master/nio-server/public/swagger/swagger.json).

## Model

@@@ div { .centered-img }
<img src="./img/nio-data-model.png"/>
@@@

### Tenant

A *Tenant* is a dedicated environment that is totally isolated from other tenants.

### Organisation

*Organisation* element represents an organisation (business organisation or any kind of organisation) that as a set of associated permissions.

### Permission group

A *Permission Group* is a set of permissions (this is actually a functionnal gathering).

### Permission

A *Permission* is an actual permission for a given authorisation (ex : "I authorise CompanyX to send me promotional emails").

### User

A *User* is a user in the scope of your organisation. User's **id** is his **id** in your organisation.

### Consents

*Consent* element stands for the actual user consent for a given permission.

## Versionning

*Permissions* and *Consents* are versionned in order to be able to handle changes in the permissions to be able to know at anytime which version of the permissions have been approuved by the user.
