#! /bin/bash -e

echo "Waiting for iRODS catalog database to be ready"
catalog_db_hostname=irods-catalog
until pg_isready -h ${catalog_db_hostname} -d ICAT -U irods -q; do
    sleep 1
done
echo "iRODS catalog database is ready"

unattended_install_file=/unattended_install.json
if [ -f "${unattended_install_file}" ]; then
    echo "Running iRODS setup"

    # Configure the server with secure communication (TLS) disabled. This
    # avoids issues with setup (e.g. the post install test).

    # Add generated hostname as a recognizable alias.
    sed -i "s/CONTAINER_HOSTNAME_ALIAS/${HOSTNAME}/g" ${unattended_install_file}
    python3 /var/lib/irods/scripts/setup_irods.py \
        --json_configuration_file ${unattended_install_file} \
        --skip_post_install_test

    # Move the input file used to configure the server out of the way so
    # the container is restartable.
    mv ${unattended_install_file} ${unattended_install_file}.processed

    # Copy TLS certificates and give the iRODS service account ownership
    # of the files.
    cp -a /tls_certs /tmp/irods_certs
    chown -R irods:irods /tmp/irods_certs

    # Update the server's configuration for secure communication.
    sed -i 's/="CS_NEG_REFUSE"/="CS_NEG_REQUIRE"/g' /etc/irods/core.re
    sed -i 's/CS_NEG_REFUSE/CS_NEG_REQUIRE/g' /etc/irods/server_config.json /var/lib/irods/.irods/irods_environment.json

    # Temporarily launch the server in the background.
    su - irods -c 'irodsServer -d'
    sleep 3

    # Enable support for the new "irods" authentication scheme and add
    # a new user. This is required by the tests targeting the new authentication scheme.
    su - irods -c 'iadmin set_grid_configuration authentication password_storage_mode both'
    su - irods -c 'iadmin mkuser alice rodsuser'
    su - irods -c 'iadmin moduser alice password apass no-scramble'

    # Shutdown the server.
    su - irods -c 'kill $(cat /run/irods/irods-server.pid)'
    sleep 3

    # Used by the docker healthcheck configuration to detect when the
    # iRODS server has completed setup.
    su - irods -c 'touch /tmp/irods_server_setup_complete.flag'
fi

echo "Starting server"
su - irods -c 'irodsServer --stdout'