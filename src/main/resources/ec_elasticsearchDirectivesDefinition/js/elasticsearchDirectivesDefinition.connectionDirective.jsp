<%@page contentType="text/javascript" %>
<%@ taglib prefix="dbc" uri="http://www.jahia.org/dbconnector/functions" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

(function () {
    'use strict';
    var elasticsearchConnectionDirective = function ($log, contextualData, dcTemplateResolver) {
        var directive = {
            restrict: 'E',
            templateUrl: function(el, attrs) {
                return dcTemplateResolver.resolveTemplatePath('${dbc:addDatabaseConnectorModulePath('/database-connector-directives/elasticsearch-directives', renderContext)}', 'connectionManagement');
            },
            controller: ElasticsearchConnectionDirectiveController,
            controllerAs: 'cecc',
            bindToController: {
                mode: '@',
                connection: '=',
                databaseType: '@',
                closeDialog: '&'
            },
            link: linkFunc
        };

        return directive;

        function linkFunc(scope, el, attr, ctrls) {
        }

    };

    angular
        .module('databaseConnector')
        .directive('elasticsearchConnectionDirective', ['$log', 'contextualData', 'dcTemplateResolver', elasticsearchConnectionDirective]);

    var ElasticsearchConnectionDirectiveController = function ($scope, contextualData,
                                                       dcDataFactory, toaster, i18n, $DCSS, $mdDialog) {
        var cecc = this;

        cecc.isEmpty = {};
        cecc.spinnerOptions = {
            showSpinner: false,
            mode: 'indeterminate'
        };
        cecc.tabLabels = {
            settings: i18n.message('dc_databaseConnector.label.settings'),
            advancedSettings: i18n.message('dc_databaseConnector.label.advancedSettings')
        };
        cecc.validations = {
            host: {
                'required': i18n.message('dc_databaseConnector.label.validation.required'),
                'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '15'),
                'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4')
            },
            port: {
                'pattern': i18n.format('dc_databaseConnector.label.validation.range', '1|65535')
            },
            id: {
                'required': i18n.message('dc_databaseConnector.label.validation.required'),
                'connection-id-validator': i18n.message('dc_databaseConnector.label.validation.connectionIdInUse'),
                'pattern': i18n.message('ec_elasticsearchConnector.label.validation.connectionId'),
                'md-maxlength': i18n.format('dc_databaseConnector.label.validation.minLength', '30')
            },
            clusterName: {
                'required': i18n.message('dc_databaseConnector.label.validation.required'),
                'pattern': i18n.message('dc_databaseConnector.label.validation.alphanumeric'),
                'md-maxlength': i18n.format('dc_databaseConnector.label.validation.minLength', '30')
            },
            user: {
                'required': i18n.message('dc_databaseConnector.label.validation.required'),
                'pattern': i18n.message('ec_elasticsearchConnector.label.validation.elasticsearch.user'),
                'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4'),
                'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '30')
            },
            password: {
                'required': i18n.message('dc_databaseConnector.label.validation.required'),
                'minlength': i18n.format('dc_databaseConnector.label.validation.minLength', '4'),
                'md-maxlength': i18n.format('dc_databaseConnector.label.validation.maxLength', '30')
            },
            transportAddress: {
                host: {
                    'required': i18n.message('dc_databaseConnector.label.validation.required')
                },
                port: {
                    'pattern': i18n.format('dc_databaseConnector.label.validation.range', '1|65535')
                }
            },
            pingTimeout: {
                'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.withUnit')
            },
            nodesSamplerInterval: {
                'pattern' : i18n.message('ec_elasticsearchConnector.label.validation.withUnit')
            }
        };

        cecc.createElasticSearchConnection = createElasticSearchConnection;
        cecc.editElasticSearchConnection = editElasticSearchConnection;
        cecc.testElasticSearchConnection = testElasticSearchConnection;
        cecc.cancel = cancel;
        cecc.updateIsEmpty = updateIsEmpty;
        cecc.updateImportedConnection = updateImportedConnection;
        cecc.initAdditionalTransportAddresses = initAdditionalTransportAddresses;
        cecc.addTransportAddress = addTransportAddress;
        cecc.removeTransportAddress = removeTransportAddress;
        cecc.updateTransportAddressesOptions = updateTransportAddressesOptions;
        cecc.updateXPackSecurity = updateXPackSecurity;

        cecc.getMessage = i18n.message;

        cecc.$onInit = function init() {console.log('es connection management controller initialized');
            if (_.isUndefined(cecc.connection.port) || cecc.connection.port == null) {
                cecc.connection.port = "9300";
            }

            /**TODO Implement when/if we implement X-Pack security handling**/

            cecc.isEmpty.password = updateIsEmpty('password');
            cecc.isEmpty.user = updateIsEmpty('user');
            if (cecc.mode === 'import-edit') {
                cecc.connection.oldId = null;
            }
            if (cecc.mode === 'edit') {
                cecc.connection.oldId = angular.copy(cecc.connection.id);
            } else {
                cecc.connection.isConnected = true;
            }
            if (_.isUndefined(cecc.connection.options) || cecc.connection.options == null || _.isString(cecc.connection.options) && cecc.connection.options.trim() == '') {
                cecc.connection.options = {};
            } else if (_.isString(cecc.connection.options)){
                cecc.connection.options = JSON.parse(cecc.connection.options);
            }

            if (cecc.connection.options.additionalTransportAddresses) {
                cecc.enableAdditionalTransportAddresses = true;
            }
        };

        function createElasticSearchConnection() {
            if (cecc.mode === 'import-edit') {
                return;
            }
            cecc.spinnerOptions.showSpinner = true;
            var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/add';

            var data = angular.copy(cecc.connection);
            var options = prepareOptions(data.options);
            if (options == null) {
                delete data.options;
            } else {
                data.options = options;
            }

            if(!cecc.connection.options.useXPackSecurity) {
                data.user = "";
                data.password = "";
            }

            dcDataFactory.customRequest({
                url: url,
                method: 'POST',
                data: data
            }).then(function (response) {
                cecc.spinnerOptions.showSpinner = false;
                showConfirmationToast(response.connectionVerified);
                $scope.closeDialog('hide');
            }, function (response) {
                cecc.spinnerOptions.showSpinner = false;
                console.log('error', response);
                toaster.pop({
                    type: 'error',
                    title: i18n.message('dc_databaseConnector.toast.title.connectionInvalid'),
                    toastId: 'cti',
                    timeout: 3000
                });
            });
        }

        function editElasticSearchConnection() {
            if (cecc.mode === 'import-edit') {
                $scope.closeDialog('hide');
                return;
            }
            cecc.spinnerOptions.showSpinner = true;
            var url = contextualData.apiUrl + '/elasticsearch/edit';
            var data = angular.copy(cecc.connection);
            var options = prepareOptions(data.options);
            if (options == null) {
                delete data.options;
            } else {
                data.options = options;
            }
            dcDataFactory.customRequest({
                url: url,
                method: 'PUT',
                data: data
            }).then(function (response) {
                cecc.spinnerOptions.showSpinner = false;
                showConfirmationToast(response.connectionVerified);
                $scope.closeDialog('hide');
            }, function (response) {
                cecc.spinnerOptions.showSpinner = false;
                console.log('error', response);
                toaster.pop({
                    type: 'error',
                    title: i18n.message('dc_databaseConnector.toast.title.connectionInvalid'),
                    toastId: 'cti',
                    timeout: 3000
                });
            });
        }

        function testElasticSearchConnection() {
            cecc.spinnerOptions.showSpinner = true;
            var url = contextualData.apiUrl + $DCSS.connectorsMetaData[cecc.databaseType].entryPoint + '/testconnection';
            var data = angular.copy(cecc.connection);
            var options = prepareOptions(data.options);
            if (options == null) {
                delete data.options;
            } else {
                data.options = options;
            }
            if (data.options == null || !data.options.useXPackSecurity) {
                data.user = "";
                data.password = "";
            }
            dcDataFactory.customRequest({
                url: url,
                method: 'POST',
                data: data
            }).then(function (response) {
                if (response.result) {
                    toaster.pop({
                        type: 'success',
                        title: i18n.message('dc_databaseConnector.toast.title.connectionValid'),
                        toastId: 'ctv',
                        timeout: 3000
                    });
                } else {
                    toaster.pop({
                        type: 'error',
                        title: i18n.message('dc_databaseConnector.toast.title.connectionInvalid'),
                        toastId: 'cti',
                        timeout: 3000
                    });
                }
                cecc.spinnerOptions.showSpinner = false;
            }, function (response) {
                console.log('error', response);
                cecc.spinnerOptions.showSpinner = false;
            });
        }

        function cancel() {
            $DCSS.selectedDatabaseType = null;
            $scope.closeDialog('cancel');
        }

        function updateIsEmpty(property) {
            return cecc.isEmpty[property] = cecc.connection[property] === undefined || cecc.connection[property] === null || (typeof cecc.connection[property] === 'string' && cecc.connection[property].trim().length === 0);
        }

        function showConfirmationToast(verified) {
            if (verified) {
                toaster.pop({
                    type: 'success',
                    title: i18n.message('dc_databaseConnector.toast.title.connectionSavedSuccessfully'),
                    body: i18n.message('dc_databaseConnector.toast.message.connectionVerificationSuccessful'),
                    toastId: 'cm',
                    timeout: 4000
                });
            } else {
                toaster.pop({
                    type: 'warning',
                    title: i18n.message('dc_databaseConnector.toast.title.connectionSavedSuccessfully'),
                    body: i18n.message('dc_databaseConnector.toast.message.connectionVerificationFailed'),
                    toastId: 'cm',
                    timeout: 4000
                });
            }
        }

        function prepareOptions(options) {

            //Transport Options
            if (!_.isEmpty(options.transport) && options.transport == null) {
                //Check what transport options have not been set and remove them.
                if (options.transport.ignoreClusterName == null) {
                    delete options.transport.ignoreClusterName
                }
                if (options.transport.pingTimeout == null || (_.isString(options.conn.pingTimeout) && options.conn.pingTimeout.trim().length == 0)) {
                    delete options.transport.pingTimeout
                }

                if (options.transport.nodesSamplerInterval == null || (_.isString(options.transport.nodesSamplerInterval) && options.transport.nodesSamplerInterval.trim().length == 0)) {
                    delete options.transport.nodesSamplerInterval
                }
                if (_.isEmpty(options.transport) || options.transport == null) {
                    delete options.transport

                }
            }

            //Additional Transport Addresses
            if (_.isEmpty(options.additionTransportAddress)) {
                delete options.additionTransportAddress
            }

            return _.isEmpty(options) ? null : JSON.stringify(options);
        }

        function updateXPackSecurity() {
            if (!cecc.connection.options.useXPackSecurity) {
                cecc.connection.user="";
                cecc.connection.password="";
            }
        }
        function updateImportedConnection() {
            if(!cecc.connection.options.useXPackSecurity) {
                cecc.connection.user="";
                cecc.connection.password="";
            }
            $scope.closeDialog('hide');
        }

        function addTransportAddress() {
            if (!_.isUndefined(cecc.connection.options.additionalTransportAddresses)) {
                cecc.connection.options.additionalTransportAddresses.push({});
            }
        }

        function removeTransportAddress(index) {
            if (!_.isUndefined(cecc.connection.options.additionalTransportAddresses)) {
                cecc.connection.options.additionalTransportAddresses.splice(index, 1);
            }
        }

        function updateTransportAddressesOptions() {
            if (cecc.enableAdditionalTransportAddresses) {
                if (_.isUndefined(cecc.connection.options.additionalTransportAddresses) || cecc.connection.options.additionalTransportAddresses == null) {
                    //create an transport address object
                    cecc.connection.options.additionalTransportAddresses = [{}];
                }
            } else {
                //remove replicaSetObject
                delete cecc.connection.options.additionalTransportAddresses
            }
        }

        function initAdditionalTransportAddresses() {
            if (!_.isUndefined(cecc.connection.options.additionalTransportAddresses) && _.isEmpty(cecc.connection.options.additionalTransportAddresses)) {
                cecc.connection.options.additionalTransportAddresses.push({});
            }
        }

    };

    ElasticsearchConnectionDirectiveController.$inject = ['$scope', 'contextualData',
        'dcDataFactory', 'toaster', 'i18nService', '$DCStateService', '$mdDialog'];

})();
