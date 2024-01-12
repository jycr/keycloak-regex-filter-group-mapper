[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/mrDFX/keycloak-regex-filter-group-mapper/blob/main/CONTRIBUTING.md)

# Keycloak OpenId Connect (OIDC) group filter mapper

Custom protocol claim mapper that adds only filtrated (by regexp) groups. You can also configure group name mapping.

## Keycloak version

This plugin currently uses the Keycloak library in version 18.0.2, so it should be compatible with Keycloak instances up
to that version and RH-SSO (≥ 7.6)

## Install

To build the jar file, simply run

```
mvn clean verify
```

If using the [official Keycloak Docker image](https://www.keycloak.org/getting-started/getting-started-docker) you can create a mount of the directory
`/opt/jboss/keycloak/standalone/deployments` and copy the jar there.

To install it with [Keycloak "on bare metal"](https://www.keycloak.org/getting-started/getting-started-zip) (from the ZIP archive):
1. Copy JAR file in directory: `$KEYCLOAK_HOME/providers/`
2. Exécute following command:
```bash
"$KEYCLOAK_HOME/bin/kc.sh" build
```

## Usage

After successful installation the mapper is available for any client in tab "Mappers" (see also in the
official [Keycloak documentation](https://www.keycloak.org/docs/latest/server_admin/index.html#_protocol-mappers)).

To create new protocol mapper, select: "Group Filter Membership (UUID)" -> insert name -> done.
In your "REALM" select "clients" -> select "client"(OIDC) f.e.: $client_name -> select "Client Scopes" -> in "Assigned
client scope" - select $client_name-dedicated -> push button "Add mapper" and select "By configuration" -> Choose mapper
type with name "Group Filter Membership" of the mappings from this table -> next in opened screen "Mapper details" MUST
to fill fields:
"Name" - any name for representation in mappers list on Keycloak client ui side;
"Token Claim Name" - Name of the claim to insert into the token ; (empty value – 5XX answers - bug/fitcha)
"Group prefix" - any PCRE regexp for filtering groups

## Example usage

In this field, you must specify the regexp by which groups within the given REALM will be filtered.
Any regular expression (PCRE) must begin with the character "^" .
Example:
if there are groups with the following names:

```
g1-a-a1
g1-a-a2
g1-b-a1
g1-b-a3
g1-c-a1
g1-c-a3
```

- and for this client, only groups are needed:

1. Beginning with g1-a-a
2. All groups starting with g1- and ending with -a3

- for this, a regexp of the type is suitable:
  ^g1-a-a.+|^g1-.+-a3.+

![Addding Group Filter Membership mapper](Group-Filter-Membership-mapper.png "")
