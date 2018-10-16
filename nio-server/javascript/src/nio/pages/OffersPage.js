import {Component} from "react";
import _ from "lodash";
import PropTypes from "prop-types";
import React from "react";
import * as organisationOfferService from "../services/OrganisationOfferService";
import {LabelInput, TextInput} from "../../common/ui/inputs";
import {Group} from "./OrganisationPage";
import {GroupPermissionPage} from "./GroupPermissionPage";
import * as errorManagerService from "../services/ErrorManagerService";


export class OffersPage extends Component {

    state = {
        offers: [],
        offersOriginalCopy: [],
        errors: []
    };

    componentDidMount() {
        this.fetch();
    }

    fetch = () => {
        organisationOfferService.getOffers(this.props.tenant, this.props.organisationKey)
            .then(maybeOffers => {
                const offers = maybeOffers ? _.cloneDeep(maybeOffers) : [];
                const offersOriginalCopy = _.cloneDeep(offers);
                this.setState({offers, offersOriginalCopy})
            });
    };

    addOffer = () => {
        if (!this.props.readOnlyMode) {
            const offer = {
                key: '',
                label: '',
                version: 1,
                groups: [
                    {
                        key: '',
                        label: '',
                        permissions: [
                            {
                                key: '',
                                label: ''
                            }
                        ]
                    }
                ]
            };
            const offers = this.state.offers ? _.cloneDeep(this.state.offers) : [];
            offers.unshift(offer);

            this.setState({offers});
        }
    };

    removeOffer = (index) => {
        if (!this.props.readOnlyMode) {
            const offers = _.cloneDeep(this.state.offers);
            offers.splice(index, 1);
            this.setState({offers});
        }
    };

    onChangeOffer = (index, offer) => {
        if (!this.props.readOnlyMode) {
            const offers = _.cloneDeep(this.state.offers);
            offers[index] = offer;

            this.setState({offers}, () => {
                if (this.state.errors && this.state.errors.length)
                    this.validate(this.state);
            });
        }
    };

    validate = (nextState) => {
        const errors = [];

        if (nextState.offers)
            nextState.offers.forEach((offer, indexOffer) => {
                if (!offer.key)
                    errors.push(`offers.${indexOffer}.key.required`);
                else if (!/^\w+$/.test(offer.key)) {
                    errors.push(`offers.${indexOffer}.key.invalid`);
                }

                if (!offer.label)
                    errors.push(`offers.${indexOffer}.label.required`);

                if (!offer.groups || !offer.groups.length)
                    errors.push(`offers.${indexOffer}.groups.required`);

                offer.groups.forEach((group, indexGroup) => {
                    if (!group.key)
                        errors.push(`offers.${indexOffer}.groups.${indexGroup}.key.required`);
                    else if (!/^\w+$/.test(group.key)) {
                        errors.push(`offers.${indexOffer}.groups.${indexGroup}.key.invalid`);
                    }

                    if (!group.label)
                        errors.push(`offers.${indexOffer}.groups.${indexGroup}.label.required`);


                    if (!group.permissions || !group.permissions.length)
                        errors.push(`offers.${indexOffer}.groups.${indexGroup}.permissions.required`);

                    group.permissions.forEach((permission, indexPermission) => {
                        if (!permission.key)
                            errors.push(`offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.key.required`);
                        else if (!/^\w+$/.test(permission.key)) {
                            errors.push(`offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.key.invalid`);
                        }

                        if (!permission.label)
                            errors.push(`offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.label.required`);
                    })
                })
            });

        this.setState({errors});
        console.log("errors ", errors);
        return errors.length === 0;
    };

    save = () => {
        if (this.validate(this.state)) {

            const offersToCreate = [];
            const offersToUpdate = [];
            const offersToDelete = [];


            this.state.offers.forEach(offer => {
                let existingOffer = this.state.offersOriginalCopy.find(o => o.key === offer.key);

                if (!existingOffer) {
                    offersToCreate.push(offer);
                } else {
                    console.log("OFFER : " + JSON.stringify(offer));
                    console.log("EXISTING OFFER : " + JSON.stringify(existingOffer));
                    if (!_.isEqual(offer, existingOffer)) {
                        offersToUpdate.push(offer);
                    }
                }
            });

            this.state.offersOriginalCopy.forEach(existingOffer => {
                let offer = this.state.offers.find(o => o.key === existingOffer.key);

                if (!offer)
                    offersToDelete.push(existingOffer);
            });

            Promise.all(
                offersToCreate.map(
                    offer => organisationOfferService.createOffer(this.props.tenant, this.props.organisationKey, offer)
                )
            ).then(
                () =>
                    Promise.all(
                        offersToUpdate.map(
                            offer => organisationOfferService.updateOffer(this.props.tenant, this.props.organisationKey, offer.key, offer)
                        )
                    ).then(
                        () =>
                            Promise.all(
                                offersToDelete.map(
                                    offer => organisationOfferService.deleteOffer(this.props.tenant, this.props.organisationKey, offer.key)
                                )
                            ).then(
                                () => this.fetch()
                            )
                    )
            );
        }
    };

    cancel = () => {
        const offers = _.cloneDeep(this.state.offersOriginalCopy);
        this.setState({offers})
    };


    render() {
        const actionButtons = (
            <div className="form-buttons pull-right btnsNewOrga">
                <button className="btn btn-danger" title="cancel" onClick={this.cancel}><i
                    className="glyphicon glyphicon-remove"/></button>

                <button className="btn btn-success" title="save" onClick={this.save}>
                    <i className="glyphicon glyphicon-hdd"/>
                </button>
            </div>

        );

        return (
            <div className="col-md-12">
                <div className="row">
                    <div className="col-md-12" style={{'marginTop': '20px'}}>
                        {
                            !this.props.readOnlyMode &&
                            <div className="btn btn-xs btn-primary pull-right" onClick={this.addOffer}>Créer une
                                offre</div>
                        }
                    </div>
                </div>
                <div className="row">
                    {
                        this.state.offers && this.state.offers.map(
                            (offer, index) =>
                                <Offer key={index} index={index} offer={offer} onChange={this.onChangeOffer}
                                       onRemove={() => this.removeOffer(index)} errors={this.state.errors}
                                       prefixe={""} readOnlyMode={this.state.readOnlyMode}/>
                        )
                    }
                </div>

                <div className="col-md-12">
                    {
                        this.state.errors && this.state.errors.length ?
                            <div className="row alert alert-danger">
                                Veuillez vérifier votre saisie :
                                {
                                    this.state.errors.map((error, index) =>
                                        <div className="col-md-12" key={index}>{errorManagerService.translate(error)}</div>
                                    )
                                }
                            </div> : ""
                    }
                </div>

                {!this.props.readOnlyMode && actionButtons}
            </div>
        )
    }
}

OffersPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string.isRequired,
    readOnlyMode: PropTypes.bool.isRequired
};


class Offer extends Component {

    state = {
        offer: {
            name: '',
            groups: []
        }
    };

    componentDidMount() {
        this.setState({offer: _.cloneDeep(this.props.offer)});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({offer: _.cloneDeep(nextProps.offer)});
    }

    onChange = (value, name) => {
        if (!this.props.readOnlyMode)
            this.setState({offer: {...this.state.offer, [name]: value}}, () => {
                this.props.onChange(this.props.index, this.state.offer);
            });
    };


    addGroup = () => {
        if (!this.props.readOnlyMode) {

            const group = {
                key: '',
                label: '',
                permissions: [{
                    key: '',
                    label: ''
                }]
            };

            const groups = _.cloneDeep(this.state.offer.groups);
            groups.unshift(group);

            this.setState({offer: {...this.state.offer, groups}}, () => this.props.onChange(this.props.index, this.state.offer));
        }
    };

    removeGroup = (index) => {
        if (!this.props.readOnlyMode) {
            const groups = _.cloneDeep(this.state.offer.groups);
            groups.splice(index, 1);
            this.setState({offer: {...this.state.offer, groups}}, () => this.props.onChange(this.props.index, this.state.offer));
        }
    };

    onChangeGroup = (index, group) => {
        if (!this.props.readOnlyMode) {
            const groups = _.cloneDeep(this.state.offer.groups);
            groups[index] = group;
            this.setState({offer: {...this.state.offer, groups}}, () => {
                this.props.onChange(this.props.index, this.state.offer);
            });
        }
    };

    render() {
        return (
            <div className="col-md-12">
                <hr/>
                <div className="form-group">
                    <div className="row">
                        <label className="col-sm-2 control-label"/>
                        <div className="col-sm-10">
                            <span className="groupe">Offre</span>
                            {
                                !this.props.readOnlyMode &&
                                <button type="button" className="btn btn-danger pull-right btn-xs"
                                        onClick={this.props.onRemove}>
                                    <i className="glyphicon glyphicon-trash"/>
                                </button>
                            }
                        </div>
                    </div>
                </div>

                <div className="row">
                    <div className="col-md-12" style={{'marginTop': '20px'}}>
                        {
                            !this.props.readOnlyMode &&
                            <div className="btn btn-xs btn-primary pull-right" onClick={this.addGroup}>Créer un groupe
                            </div>
                        }
                    </div>
                </div>

                <LabelInput label={"Version"} value={this.state.offer.version}/>

                <TextInput label={"Clé de l'offre"} value={this.state.offer.key}
                           onChange={(e) => this.onChange(e, "key")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={[`${this.props.prefixe}offers.${this.props.index}.key.required`, `${this.props.prefixe}offers.${this.props.index}.key.invalid`]}/>

                <TextInput label={"Libellé de l'offre"} value={this.state.offer.label}
                           onChange={(e) => this.onChange(e, "label")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={[`${this.props.prefixe}offers.${this.props.index}.label.required`]}/>
                {
                    this.state.offer.groups.map(
                        (group, index) =>
                            <GroupPermissionPage key={index} index={index} group={group} onChange={this.onChangeGroup}
                                   onRemove={() => this.removeGroup(index)} readOnlyMode={this.props.readOnlyMode}
                                   prefixe={`${this.props.prefixe}offers.${this.props.index}.`}
                                   errors={this.props.errors}/>
                    )
                }

            </div>
        )
    }
}

Offer.propTypes = {
    offer: PropTypes.object.isRequired,
    index: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired,
    readOnlyMode: PropTypes.bool,
    prefixe: PropTypes.string,
    errors: PropTypes.array
};