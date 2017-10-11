<dc-spinner spinner-mode="cecc.spinnerOptions.mode" show="cecc.spinnerOptions.showSpinner"></dc-spinner>
<md-content layout-padding>
    <md-tabs md-dynamic-height md-border-bottom>
        <md-tab label="{{cecc.tabLabels.settings}}">
            <form name="elasticsearchForm">
                <md-content class="md-padding">
                    <md-checkbox ng-model="cecc.connection.isConnected" aria-label="Automatic startup after Creation"
                                 ng-true-value="true" ng-false-value="false"
                                 class="md-warn md-align-top-left" flex>
                        <span class="ipsum" message-key="dc_databaseConnector.label.connection.enableConnection"></span>
                    </md-checkbox>

                    <md-button class="md-fab md-mini md-success pull-right"
                               ng-click="cecc.testElasticSearchConnection()"
                               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid">
                        <i class="fa fa-lg fa-check-circle-o"></i>
                        <md-tooltip class="noToolTipAnimation"
                                    md-direction="top"
                                    md-delay="500">
                            <span message-key="dc_databaseConnector.label.connection.testConnection"></span>
                        </md-tooltip>
                    </md-button>
                    <md-input-container class="md-block" flex-gt-sm="1">
                        <label message-key="ec_elasticsearchConnector.label.host"></label>
                        <input title="host" required ng-minlength="4" name="host"
                               ng-model="cecc.connection.host">
                        <div ng-messages="elasticsearchForm.host.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.host"
                                 ng-class="{'showErrorMessages': elasticsearchForm.host.$invalid && elasticsearchForm.host.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.port"></label>
                        <input title="port" md-maxlength="5" ng-pattern="/[0-9]*/" name="port"
                               ng-model="cecc.connection.port" length-parser length-parser-max="5" range-parser
                               range-max-value="65535">
                        <div ng-messages="elasticsearchForm.port.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.port"
                                 ng-class="{'showErrorMessages': elasticsearchForm.port.$invalid && elasticsearchForm.port.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.id"></label>
                        <input title="Connection Name" md-maxlength="30" name="id" required
                               ng-readonly="cecc.mode === 'edit'"
                               ng-pattern="/^[\w]+[\w\-]+[\w]+$/"
                               ng-model="cecc.connection.id" original-value="{{cecc.connection.oldId}}"
                               connection-id-validator="{{cecc.databaseType}}">
                        <div ng-messages="elasticsearchForm.id.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.id"
                                 ng-class="{'showErrorMessages': elasticsearchForm.id.$invalid && elasticsearchForm.id.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>

                    <md-input-container class="md-block">
                        <label message-key="ec_elasticsearchConnector.label.clusterName"></label>
                        <input title="Database Name" md-maxlength="30" required ng-pattern="/^[\w]+[\w\-]+[\w]+$/"
                               name="clusterName"
                               ng-model="cecc.connection.clusterName">
                        <div ng-messages="elasticsearchForm.clusterName.$error">
                            <div ng-message-exp="validationName"
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.clusterName"
                                 ng-class="{'showErrorMessages': elasticsearchForm.clusterName.$invalid && elasticsearchForm.clusterName.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                </md-content>
            </form>
        </md-tab>

        <md-tab label="{{cecc.tabLabels.advancedSettings}}">
            <md-content class="md-padding">
                <form name="elasticsearchAdvancedForm">
                    <!-- ***START*  XPACK SECURITY OPTIONS -->
                    <%--<md-subheader class=" md-no-sticky">--%>
                        <%--<span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.useXPackSecurity"></span>--%>
                        <%--<md-checkbox class="pull-right"--%>
                                     <%--ng-model="cecc.connection.options.useXPackSecurity"--%>
                                     <%--ng-true-value="true"--%>
                                     <%--ng-false-value="false"--%>
                                     <%--ng-change="cecc.updateXPackSecurity()">--%>
                        <%--</md-checkbox>--%>
                    <%--</md-subheader>--%>
                    <%--<md-input-container class="md-block col-md-offset-2" ng-if="cecc.connection.options.useXPackSecurity">--%>
                        <%--<label message-key="ec_elasticsearchConnector.label.user"></label>--%>
                        <%--<input title="User" name="user" required ng-minlength="4" md-maxlength="30"--%>
                               <%--ng-model="cecc.connection.user"--%>
                               <%--ng-pattern="/^[a-zA-Z0-9_-]*$/"--%>
                               <%--ng-change="cecc.updateIsEmpty('user');">--%>
                        <%--<div ng-messages="elasticsearchAdvancedForm.user.$error">--%>
                            <%--<div ng-message-exp="validationName"--%>
                                 <%--ng-repeat="(validationName, validationMessage) in cecc.validations.user"--%>
                                 <%--ng-class="{'showErrorMessages': elasticsearchAdvancedForm.user.$invalid && elasticsearchAdvancedForm.user.$touched}">--%>
                                <%--{{validationMessage}}--%>
                            <%--</div>--%>
                        <%--</div>--%>
                    <%--</md-input-container>--%>

                    <%--<md-input-container class="md-block col-md-offset-2" ng-if="!cecc.isEmpty.user && cecc.connection.options.useXPackSecurity">--%>
                        <%--<label message-key="ec_elasticsearchConnector.label.password"></label>--%>
                        <%--<input title="Password" type="password" name="password" ng-model="cecc.connection.password"--%>
                               <%--required ng-minlength="4" md-maxlength="30"--%>
                               <%--ng-change="cecc.updateIsEmpty('password');">--%>
                        <%--<div ng-messages="elasticsearchAdvancedForm.password.$error">--%>
                            <%--<div ng-message-exp="validationName"--%>
                                 <%--ng-repeat="(validationName, validationMessage) in cecc.validations.password"--%>
                                 <%--ng-class="{'showErrorMessages': elasticsearchAdvancedForm.password.$invalid && elasticsearchAdvancedForm.password.$touched}">--%>
                                <%--{{validationMessage}}--%>
                            <%--</div>--%>
                        <%--</div>--%>
                    <%--</md-input-container>--%>
                    <!-- ***END*  XPACK SECURITY OPTIONS -->
                    <%--<md-divider></md-divider>--%>
                    <!-- ***START*  TRANSPORT OPTIONS -->
                    <md-subheader class="md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.transportSettings"></span>
                    </md-subheader>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.ignoreClusterName"></label>
                        <md-checkbox class="pull-right"
                                     ng-model="cecc.connection.options.transport.ignoreClusterName"
                                     ng-true-value="true"
                                     ng-false-value="false">
                        </md-checkbox>
                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.pingTimeout"></label>
                        <input title="Connect Timeout" name="pingTimeout"
                               ng-pattern="/^[0-9]+[a-z]{1}$/"
                               ng-model="cecc.connection.options.transport.pingTimeout">
                        <div ng-messages="elasticsearchAdvancedForm.pingTimeout.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.pingTimeout"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.pingTimeout.$invalid && elasticsearchAdvancedForm.pingTimeout.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <md-input-container class="md-block col-md-offset-2">
                        <label message-key="ec_elasticsearchConnector.label.modal.elasticsearch.nodesSamplerInterval"></label>
                        <input title="Nodes Sampler Interval" name="nodesSamplerInterval"
                               ng-pattern="/^[0-9]+[a-z]{1}$/"
                               ng-model="cecc.connection.options.transport.nodesSamplerInterval">
                        <div ng-messages="elasticsearchAdvancedForm.nodesSamplerInterval.$error">
                            <div ng-message-exp='validationName'
                                 ng-repeat="(validationName, validationMessage) in cecc.validations.nodesSamplerInterval"
                                 ng-class="{'showErrorMessages': elasticsearchAdvancedForm.nodesSamplerInterval.$invalid && elasticsearchAdvancedForm.nodesSamplerInterval.$touched}">
                                {{validationMessage}}
                            </div>
                        </div>
                    </md-input-container>
                    <!-- ***END*  TRANSPORT OPTIONS -->
                    <md-divider></md-divider>

                    <!-- ***START*  ADDITIONAL TRANSPORT ADDRESSES OPTION -->
                    <md-subheader class=" md-no-sticky">
                        <span message-key="ec_elasticsearchConnector.label.modal.elasticsearch.additionalTransportAddresses"></span>
                        <md-checkbox class="pull-right"
                                     ng-model="cecc.enableAdditionalTransportAddresses"
                                     ng-true-value="true"
                                     ng-false-value="false"
                                     ng-change="cecc.updateTransportAddressesOptions()">
                        </md-checkbox>
                    </md-subheader>
                    <div ng-if="cecc.enableAdditionalTransportAddresses">
                        <md-list>
                            <md-list-item ng-repeat="transportAddress in cecc.connection.options.additionalTransportAddresses track by $index "
                                          layout-margin layout-padding layout-fill layout-wrap="" class="box-wrap2">
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="ec_elasticsearchConnector.label.host"></label>
                                    <input title="transportAddressHost" class="md-primary" required
                                           name="transportAddressHost_{{$index}}"
                                           ng-model="transportAddress.host">
                                    <div ng-messages="elasticsearchAdvancedForm['transportAddressHost_' + $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.transportAddress.host"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['transportAddressHost_' + $index].$invalid && elasticsearchAdvancedForm['transportAddressHost_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                :
                                <md-input-container class="md-block" layout="flex">
                                    <label message-key="dc_databaseConnector.label.port"></label>
                                    <input title="transportAddressPort" class="md-primary"
                                           name="transportAddressPort_{{$index}}"
                                           ng-pattern="/^[0-9]*$/"
                                           ng-model="transportAddress.port " length-parser length-parser-max="5" range-parser
                                           range-max-value="65535">
                                    <div ng-messages="elasticsearchAdvancedForm['transportAddressPort_' + $index].$error">
                                        <div ng-message-exp='validationName'
                                             ng-repeat="(validationName, validationMessage) in cecc.validations.transportAddress.port"
                                             ng-class="{'showErrorMessages': elasticsearchAdvancedForm['transportAddressPort_' + $index].$invalid && elasticsearchAdvancedForm['transportAddressPort_' + $index].$touched}">
                                            {{validationMessage}}
                                        </div>
                                    </div>
                                </md-input-container>
                                <md-icon class="md-secondary"
                                         title="Remove Transport Address"
                                         ng-click="cecc.removeTransportAddress($index)">
                                    <i class="fa fa-trash-o"></i>
                                </md-icon>
                            </md-list-item>
                            <md-list-item>
                                <md-icon class="md-secondary"
                                         title="Add Transport Address"
                                         ng-click="cecc.addTransportAddress()">
                                    <i class="fa fa-plus"></i>
                                </md-icon>
                            </md-list-item>
                        </md-list>
                    </div>
                    <!-- ***END*  ADDITIONAL TRANSPORT ADDRESSES OPTION -->
                </form>
            </md-content>
        </md-tab>
    </md-tabs>
    <md-button ng-if="cecc.mode=='create'"
               class="md-raised md-primary pull-right"
               ng-click="cecc.createElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="dc_databaseConnector.label.create">
    </md-button>
    <md-button ng-if="cecc.mode=='edit'"
               class="md-raised md-primary pull-right"
               ng-click="cecc.editElasticSearchConnection()"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               message-key="dc_databaseConnector.label.update">
    </md-button>
    <md-button class="md-raised md-primary pull-right"
               ng-disabled="elasticsearchForm.$invalid || elasticsearchAdvancedForm.$invalid"
               ng-if="cecc.mode=='import-edit'"
               ng-click="cecc.updateImportedConnection()"
               message-key="dc_databaseConnector.label.ok">
    </md-button>
    <md-button class="md-raised md-primary pull-right"
               ng-click="cecc.cancel()">
        <span ng-if="cecc.mode=='edit' || cecc.mode=='create'"
              message-key="dc_databaseConnector.label.back"></span>
        <span ng-if="cecc.mode=='import-edit'"
              message-key="dc_databaseConnector.label.cancel"></span>
    </md-button>
</md-content>