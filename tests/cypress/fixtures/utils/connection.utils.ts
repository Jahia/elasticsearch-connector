export const testConnection = (expected: boolean): void => {
    cy.apollo({
        queryFile: 'graphql/testConnection.graphql'
    }).then(resp => {
        return resp.data.admin.elasticsearch.testConnection;
    }).then((result: boolean) => {
        expect(result).to.equal(expected);
    });
};

export const editConfigValue = (key: string, value: string, pid: string = 'org.jahia.modules.elasticsearchConnector') : void => {
    cy.apollo({
        mutationFile: 'graphql/editConfigValue.graphql',
        variables: {pid, key, value}
    })
        .then(() => getConfigValue(key))
        .then(result => {
            expect(result).to.equal(value);
        });
};

export const getConfigValue = (key: string, pid: string = 'org.jahia.modules.elasticsearchConnector') => {
    return cy.apollo({
        mutationFile: 'graphql/getConfigValue.graphql',
        variables: {pid, key}
    }).then(resp => {
        return resp.data.admin.jahia.configuration.value;
    });
};

export const deleteConfigValue = (key: string, pid: string = 'org.jahia.modules.elasticsearchConnector') => {
    return cy.apollo({
        mutationFile: 'graphql/deleteConfigValue.graphql',
        variables: {pid, key}
    });
};
