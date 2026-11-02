#!/bin/bash
set -e
./mvnw test
echo "[SUCCESS] All Aegis Mesh Pay unit and integration tests passed!"
