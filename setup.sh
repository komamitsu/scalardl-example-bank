#!/usr/bin/env bash

set -euxo pipefail

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
