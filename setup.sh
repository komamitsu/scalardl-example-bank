#!/usr/bin/env bash

set -euxo pipefail

rm -rf certs
mkdir -p certs

pushd certs
cat << EOF > client-cert.json
{
    "CN": "client.example",
    "key": {
        "algo": "ecdsa",
        "size": 256
    },
    "names": [
        {
            "O": "Client Example",
            "L": "Shinjuku",
            "ST": "Tokyo",
            "C": "JP"
        }
    ]
}
EOF
cfssl selfsign "" ./client-cert.json | cfssljson -bare client
pushd

cat client.properties.tmpl | envsubst > client.properties

