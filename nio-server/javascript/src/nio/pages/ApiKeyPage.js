import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as apiKeyService from "../services/ApiKeyService";
import {BooleanInput, TextInput} from "../../common/ui/inputs";

export class ApiKeyPage extends Component {

    state = {
        apiKeyId: '',
        apiKey: {
            clientId: '',
            clientSecret: '',
            offerRestrictionPatterns: []
        },
        errors: []
    };

    componentDidMount() {
        if (this.props.apiKeyId)
            this.fetchApiKey(this.props.apiKeyId)
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.apiKeyId)
            this.fetchApiKey(nextProps.apiKeyId)
    }

    fetchApiKey = (apiKeyId) => {
        if (apiKeyId)
            apiKeyService.getApiKey(apiKeyId)
                .then(apiKey => this.setState({apiKey, apiKeyId}));
        else
            this.setState({
                apiKey: {
                    clientId: '',
                    clientSecret: '',
                    offerRestrictionPatterns: []
                }
            });
    };

    onChange = (value, name) => {
        this.setState({apiKey: {...this.state.apiKey, [name]: value}}, () => {
            if (this.state.errors && this.state.errors.length)
                this.validate(this.state);
        });
    };

    onChangeOfferPatterns = (value, name) => {
        this.setState({apiKey: {...this.state.apiKey, [name]: value.split(",").map(v => v.trim())}}, () => {
            if (this.state.errors && this.state.errors.length)
                this.validate(this.state);
        });
    };

    validate = (state) => {
        const errors = [];

        if (!state.apiKeyId) {
            const regexClient = /^[a-zA-Z\-0-9]*$/;

            if (!state.apiKey.clientId)
                errors.push("apiKey.clientId.required");
            else if (!regexClient.test(state.apiKey.clientId))
                errors.push("apiKey.clientId.invalidFormat");

            if (!state.apiKey.clientSecret)
                errors.push("apiKey.clientSecret.required");
            else if (!regexClient.test(state.apiKey.clientSecret))
                errors.push("apiKey.clientSecret.invalidFormat");
        }
        this.setState({errors});

        return errors.length === 0;
    };

    save = () => {
        if (this.validate(this.state)) {
            const apiKey = {...this.state.apiKey};

            if (apiKey.offerRestrictionPatterns && !apiKey.offerRestrictionPatterns.length)
                delete apiKey.offerRestrictionPatterns;

            if (this.state.apiKeyId)
                apiKeyService.updateApiKey(this.state.apiKeyId, apiKey)
                    .then(r => this.fetchApiKey(r._id));
            else
                apiKeyService.createApiKey(apiKey)
                    .then(r => this.fetchApiKey(r._id));
        }
    };

    render() {
        const updateMode = !!this.state.apiKeyId;

        return (
            <div className="row">
                <div className="col-md-12">
                    <h3>Api key</h3>
                </div>

                <div className="col-md-12">

                    <TextInput
                        label={"Client id"}
                        value={this.state.apiKey.clientId}
                        onChange={(e) => this.onChange(e, "clientId")}
                        errorMessage={this.state.errors}
                        errorKey={["apiKey.clientId.required", "apiKey.clientId.invalidFormat"]}
                        disabled={updateMode}
                    />

                    <TextInput
                        label={"Client secret"}
                        value={this.state.apiKey.clientSecret}
                        onChange={(e) => this.onChange(e, "clientSecret")}
                        errorMessage={this.state.errors}
                        errorKey={["apiKey.clientSecret.required", "apiKey.clientSecret.invalidFormat"]}
                        disabled={updateMode}
                    />

                        <TextInput
                            label={"Pattern d'offre disponible"}
                            value={(this.state.apiKey.offerRestrictionPatterns || []).join(", ")}
                            onChange={(e) => this.onChangeOfferPatterns(e, "offerRestrictionPatterns")}
                            errorMessage={this.state.errors}
                            errorKey={[]}
                        />

                </div>
                <div className="col-md-12">
                    <div className="form-buttons pull-right">
                        <button className="btn btn-primary" onClick={this.save}>
                            Enregistrer
                        </button>
                    </div>
                </div>
            </div>
        )
    }
}

ApiKeyPage.propTypes = {
    apiKeyId: PropTypes.string
};