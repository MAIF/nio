import React, {Component} from 'react';
import PropTypes from "prop-types";

import * as userExtractService from "../services/UserExtractService";
import {TextInput} from "../../common/ui/inputs";

export class UploadFilePage extends Component {

    state = {
        organisationKey: '',
        userId: '',
        fileUrl: '',
        errors: [],
        data: null,
        fileName: '',
        type: ''
    };

    componentDidMount() {
        this.setState({organisationKey: this.props.organisationKey, userId: this.props.userId});
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.organisationKey !== this.props.organisationKey)
            this.setState({organisationKey: nextProps.organisationKey});

        if (nextProps.userId !== this.props.userId)
            this.setState({userId: nextProps.userId});
    }


    handleUploadFile = (event) => {
        const data = new FormData();
        data.append('file', event.target.files[0]);
        data.append('name', event.target.files[0].name);

        this.setState({data, fileName: event.target.files[0].name, type: event.target.files[0].type});
    };


    submit = () => {
        if (this.validate(this.state))
            userExtractService.uploadFile(this.props.tenant, this.state.organisationKey, this.state.userId, this.state.data, this.state.fileName)
                .then(fileUrl => {
                        this.setState({fileUrl: fileUrl.url});
                        this.props.onUpload(fileUrl.url, this.state.userId);
                    }
                )
    };

    onChange = (value, name) => {
        this.setState({[name]: value}, () => {
            if (this.state.errors && this.state.errors.length)
                this.validate(this.state);
        });
    };

    validate = (nextState) => {
        const errors = [];

        if (!nextState.organisationKey)
            errors.push("consentsSample.organisationKey.required");
        if (!nextState.userId)
            errors.push("consentsSample.userId.required");
        if (!nextState.data)
            errors.push("consentSample.data.required");

        this.setState({errors});

        return errors.length === 0;
    };

    render() {
        return (
            <div className="row">
                <div className="col-md-12">
                    <h3>Téléchargement du fichier</h3>
                </div>

                <div className="col-md-12">
                    <TextInput
                        label={"Clé de l'organisation"}
                        value={this.state.organisationKey}
                        onChange={(e) => this.onChange(e, "organisationKey")}
                        errorMessage={this.state.errors}
                        errorKey={["consentsSample.organisationKey.required", "consentsSample.organisationKey.not.found"]}

                    />
                    <TextInput
                        label={"Identifiant de l'utilisateur"}
                        value={this.state.userId}
                        onChange={(e) => this.onChange(e, "userId")}
                        errorMessage={this.state.errors}
                        errorKey={"consentsSample.userId.required"}
                    />
                </div>

                <div className="col-md-12">
                    <input type="file" onChange={this.handleUploadFile}/>

                    {
                        this.state.errors && this.state.errors.indexOf("consentSample.data.required") !== -1 &&
                        <div>fichier manquant</div>

                    }
                </div>

                <div className="col-md-12">
                    <div className="form-buttons pull-right">
                        <button className="btn btn-primary" onClick={this.submit}>
                            Charger le fichier
                        </button>
                    </div>
                </div>


                {
                    this.state.fileUrl &&
                    <div className="col-md-12">
                        <a href={this.state.fileUrl} target="_blank">Accès au fichier téléchargé</a>
                    </div>
                }

            </div>
        )
    }
}

UploadFilePage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string.isRequired,
    userId: PropTypes.string.isRequired,
    onUpload: PropTypes.func.isRequired
};