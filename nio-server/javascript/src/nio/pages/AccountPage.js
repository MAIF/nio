import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as accountService from "../services/AccountService";
import {BooleanInput, TextInput} from "../../common/ui/inputs";

export class AccountPage extends Component {

    state = {
        accountId: '',
        account: {
            email: '',
            password: '',
            confirmPassword: '',
            isAdmin: false,
            offerRestrictionPatterns: []
        },
        errors: []
    };

    componentDidMount() {
        if (this.props.accountId)
            this.fetchAccount(this.props.accountId)
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.accountId)
            this.fetchAccount(nextProps.accountId)
    }

    fetchAccount = (accountId) => {
        if (accountId)
            accountService.getAccount(accountId)
                .then(account => this.setState({account, accountId}));
        else
            this.setState({
                account: {
                    email: '',
                    password: '',
                    confirmPassword: '',
                    isAdmin: false,
                    offerRestrictionPatterns: []
                }
            });
    };

    onChange = (value, name) => {
        this.setState({account: {...this.state.account, [name]: value}}, () => {
            if (this.state.errors && this.state.errors.length)
                this.validate(this.state);
        });
    };

    onChangeOfferPatterns = (value, name) => {
        this.setState({account: {...this.state.account, [name]: value.split(",").map(v => v.trim())}}, () => {
            if (this.state.errors && this.state.errors.length)
                this.validate(this.state);
        });
    };

    validate = (state) => {
        const errors = [];

        if (!state.accountId) {
            if (!state.account.email) {
                errors.push("account.email.required");
            } else {
                const re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

                if (!re.test(state.account.email)) {
                    errors.push("account.email.invalidFormat")
                }
            }

            if (!state.account.password)
                errors.push("account.password.required");

            if (!state.account.confirmPassword)
                errors.push("account.confirmPassword.required");

            if (state.account.password && state.account.confirmPassword && state.account.password !== state.account.confirmPassword)
                errors.push("account.confirmPassword.invalid");
        }
        this.setState({errors});

        return errors.length === 0;
    };

    save = () => {
        if (this.validate(this.state)) {
            const account = {...this.state.account};

            if (account.offerRestrictionPatterns && !account.offerRestrictionPatterns.length)
                delete account.offerRestrictionPatterns;

            if (account.confirmPassword)
                delete account.confirmPassword;

            if (account.isAdmin)
                account.offerRestrictionPatterns = ["*"];

            if (this.state.accountId)
                accountService.updateAccount(this.state.accountId, account)
                    .then(r => this.fetchAccount(r._id));
            else
                accountService.createAccount(account)
                    .then(r => this.fetchAccount(r._id));
        }
    };

    render() {
        const updateMode = !!this.state.accountId;

        return (
            <div className="row">
                <div className="col-md-12">
                    <h3>Compte</h3>
                </div>

                <div className="col-md-12">
                    <TextInput
                        label={"Email"}
                        value={this.state.account.email}
                        onChange={(e) => this.onChange(e, "email")}
                        errorMessage={this.state.errors}
                        errorKey={["account.email.required", "account.email.invalidFormat"]}
                        disabled={updateMode}
                    />

                    <TextInput
                        label={"Mot de passe"}
                        value={updateMode ? "updateMode" : this.state.account.password}
                        onChange={(e) => this.onChange(e, "password")}
                        errorMessage={this.state.errors}
                        errorKey={["account.password.required"]}
                        type="password"
                        disabled={updateMode}
                    />

                    <TextInput
                        label={"Confirmation Mot de passe"}
                        value={updateMode ? "updateMode" : this.state.account.confirmPassword}
                        onChange={(e) => this.onChange(e, "confirmPassword")}
                        errorMessage={this.state.errors}
                        errorKey={["account.confirmPassword.required", "account.confirmPassword.invalid"]}
                        type="password"
                        disabled={updateMode}
                    />

                    {
                        !this.state.account.isAdmin &&
                        <TextInput
                            label={"Pattern d'offre disponible"}
                            value={(this.state.account.offerRestrictionPatterns || []).join(", ")}
                            onChange={(e) => this.onChangeOfferPatterns(e, "offerRestrictionPatterns")}
                            errorMessage={this.state.errors}
                            errorKey={[]}
                        />
                    }


                    <BooleanInput value={this.state.account.isAdmin}
                                  onChange={(v) => this.onChange(v, "isAdmin")}
                                  label={"Administrateur"}
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

AccountPage.propTypes = {
    accountId: PropTypes.string
};