import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as consentService from "../services/ConsentService";
import {TextInput} from "../../common/ui/inputs";
import {ConsentsPage} from "./Consents";
import Moment from 'react-moment';
import 'moment/locale/fr'

export class SampleConsentsPage extends Component {
    state = {
        organisationKey: this.props.organisationKey || '',
        userId: this.props.userId || '',
        user: null,
        templateUser: null,
        errors: [],
        newTemplate: false
    };

    componentDidMount() {
        this.setState({organisationKey: this.props.organisationKey || '', userId: this.props.userId || ''}, () => {
            if (this.props.organisationKey && this.props.userId)
                this.getConsents();
        })
    }


    getConsents = () => {
        if (this.validate(this.state))
            consentService.getConsentsTemplate(this.props.tenant, this.state.organisationKey, this.state.userId)
                .then(r => {
                    if (r.status === 404) {
                        const errors = [];
                        errors.push("consentsSample.organisationKey.not.found");
                        this.setState({errors});

                        console.log(errors)
                    } else {
                        r.json().then(templateUser => {
                            if (templateUser.lastUpdate)
                                delete templateUser.lastUpdate;

                            this.setState({templateUser}, () => {
                                consentService.getConsents(this.props.tenant, this.state.organisationKey, this.state.userId)
                                    .then(r => {
                                        if (r.status === 404) {
                                            this.setState({user: {...templateUser}, newTemplate: true});
                                        } else {
                                            r.json().then(user => {
                                                this.setState({user, newTemplate: false})
                                            })
                                        }
                                    });
                            });
                        });
                    }
                });
    };

    onChange = (value, name) => {
        this.setState({[name]: value, user: null}, () => {
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

        this.setState({errors});

        return errors.length === 0;
    };

    compareWithCurrent = () => {
        if (this.state.templateUser && this.state.user) {
            if (this.state.templateUser.version !== this.state.user.version)
                return true;
            else {
                if (this.state.templateUser.offers && !this.state.user.offers) {
                    return true;
                } else if (!this.state.templateUser.offers && this.state.user.offers) {
                    return true;
                } else {
                    if (this.state.templateUser.offers.length !== this.state.user.offers.length)
                        return true;
                    else {
                        let templateOfferKeys = this.state.templateUser.offers.map(o => o.key).sort();
                        let userOfferKeys = this.state.user.offers.map(o => o.key).sort();

                        if (_.eq(templateOfferKeys, userOfferKeys)) {
                            return true;
                        } else {
                            let diff = false;
                            this.state.templateUser.offers.forEach(o => {
                                if (!diff && this.state.user.offers.find(uo => uo.key === o.key && uo.version !== o.version))
                                    diff = true
                            });

                            return diff;
                        }
                    }
                }
            }
        }
    };

    render() {
        const newTemplateAvailable = this.compareWithCurrent();

        return (
            <div className="row">
                <div className="col-md-12">
                    <h3>Consentements</h3>
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
                    <div className="form-buttons pull-right">
                        <button className="btn btn-primary" onClick={this.getConsents}>
                            Afficher
                        </button>
                    </div>
                </div>
                <div className="col-md-12">
                    {
                        this.state.errors && this.state.errors.length ?
                            <div className="alert alert-danger">
                                Veuillez vérifier votre saisie.
                            </div> : ""
                    }
                </div>

                {
                    this.state.user &&
                    <div className="col-md-12">
                        {
                            this.state.newTemplate ?
                                <h3>Récupération d'un nouveau template de consentement basé sur l'organisation</h3>
                                :
                                <h3>Récupération des consentements de l'utilisateur {
                                    newTemplateAvailable &&
                                    <div className="btn btn-primary"
                                         onClick={() => this.setState({user: {...this.state.templateUser}})}>Mettre à
                                        jour les consentements
                                        avec la version courante</div>
                                }
                                </h3>
                        }

                        <div className="row">

                            {
                                this.state.user.lastUpdate &&

                                <div className="col-md-2">
                                    Date de dernière mise à jour
                                </div>
                            }
                            {
                                this.state.user.lastUpdate &&
                                <div className="col-md-8">
                                    <Moment locale="fr" parse="YYYY-MM-DDTHH:mm:ssZ"
                                            format="DD/MM/YYYY HH:mm:ss">{this.state.user.lastUpdate}</Moment> (<Moment
                                    locale="fr"
                                    parse="YYYY-MM-DDTHH:mm:ssZ"
                                    fromNow>{this.state.user.lastUpdate}</Moment>)
                                </div>
                            }
                        </div>
                    </div>
                }

                {
                    this.state.user &&
                    <ConsentsPage groups={this.state.user.groups} offers={this.state.user.offers}
                                  user={this.state.user} tenant={this.props.tenant}
                                  organisationKey={this.state.organisationKey} userId={this.state.userId}
                                  submitable={true} onSave={this.getConsents}/>
                }
            </div>
        )
    }
}

SampleConsentsPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string,
    userId: PropTypes.string
};
