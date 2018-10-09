import React, {Component} from 'react';
import PropTypes from "prop-types";
import {LabelInput, TextInput} from "../../common/ui/inputs";
import * as organisationService from "../services/OrganisationService";
import {ConsentsPage} from "./Consents";

export class OrganisationPage extends Component {

    state = {
        organisation: {
            key: '',
            version: {
                status: '',
                num: ''
            },
            label: '',
            groups: [],
            offers: []
        },
        loading: true,
        visualizeConsents: false,
        haveReleases: true
    };

    componentDidMount() {
        this.fetch(this.props);
    };

    componentWillReceiveProps(nextProps) {
        this.fetch(nextProps);
    }

    fetch = (nextProps) => {
        if (nextProps.organisationKey) {
            organisationService.getOrganisationReleasesHistoric(nextProps.tenant, nextProps.organisationKey)
                .then(releases => this.setState({haveReleases: releases.length}))
        }

        if (nextProps.organisationKey && !nextProps.version) {
            this.setState({loading: true}, () => {
                    organisationService.getOrganisationDraft(nextProps.tenant, nextProps.organisationKey)
                        .then(organisation => {
                            this.setState({organisation, loading: false});
                        })
                }
            );
        } else if (nextProps.organisationKey && nextProps.version) {
            this.setState({loading: true}, () => {
                    organisationService.getOrganisationReleaseVersion(nextProps.tenant, nextProps.organisationKey, nextProps.version)
                        .then(organisation => {
                            this.setState({organisation, loading: false});
                        })
                }
            );
        } else {
            this.setState({loading: true}, () => {
                const organisation = {
                    key: '',
                    version: {
                        status: '',
                        num: ''
                    },
                    label: '',
                    groups: [{
                        key: '',
                        label: '',
                        permissions:
                            [
                                {
                                    key: '',
                                    label: ''
                                }
                            ]
                    }],
                    offers: []
                };

                this.setState({organisation, loading: false});
            })
        }
    };

    addGroup = () => {
        if (!this.props.readOnlyMode) {

            const group = {
                key: '',
                label: '',
                permissions:
                    [
                        {
                            key: '',
                            label: ''
                        }
                    ]
            };

            const groups = [...this.state.organisation.groups];
            groups.unshift(group);

            this.setState({organisation: {...this.state.organisation, groups}});
        }
    };

    removeGroup = (index) => {
        if (!this.props.readOnlyMode) {
            const groups = [...this.state.organisation.groups];
            groups.splice(index, 1);
            this.setState({organisation: {...this.state.organisation, groups}});
        }
    };

    onChangeGroup = (index, group) => {
        if (!this.props.readOnlyMode) {
            const groups = [...this.state.organisation.groups];
            groups[index] = group;
            this.setState({organisation: {...this.state.organisation, groups}}, () => {
                if (this.state.errors && this.state.errors.length)
                    this.validate(this.state);
            });
        }

    };

    addOffer = () => {
        if (!this.props.readOnlyMode) {
            const offer = {
                key: '',
                label: '',
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
            const offers = [...this.state.organisation.offers || []];
            offers.unshift(offer);

            this.setState({organisation: {...this.state.organisation, offers}});
        }
    };

    removeOffer = (index) => {
        if (!this.props.readOnlyMode) {
            const offers = [...this.state.organisation.offers];
            offers.splice(index, 1);
            this.setState({organisation: {...this.state.organisation, offers}});
        }
    };

    onChangeOffer = (index, offer) => {
        if (!this.props.readOnlyMode) {
            const offers = [...this.state.organisation.offers];
            offers[index] = offer;

            this.setState({organisation: {...this.state.organisation, offers}}, () => {
                if (this.state.errors && this.state.errors.length)
                    this.validate(this.state);
            });
        }
    };

    onChange = (value, name) => {
        if (!this.props.readOnlyMode)
            this.setState({organisation: {...this.state.organisation, [name]: value}}, () => {
                if (this.state.errors && this.state.errors.length)
                    this.validate(this.state);
            });
    };

    validate = (nextState) => {
        const errors = [];

        if (!nextState.organisation.key) {
            errors.push("organisation.key.required");
        } else if (!/^\w+$/.test(nextState.organisation.key)) {
            errors.push("organisation.key.invalid");
        }

        if (!nextState.organisation.label)
            errors.push("organisation.label.required");

        nextState.organisation.groups.forEach((group, indexGroup) => {
            if (!group.key)
                errors.push(`organisation.groups.${indexGroup}.key.required`);
            else if (!/^\w+$/.test(group.key)) {
                errors.push(`organisation.groups.${indexGroup}.key.invalid`);
            }

            if (!group.label)
                errors.push(`organisation.groups.${indexGroup}.label.required`);

            group.permissions.forEach((permission, indexPermission) => {
                if (!permission.key)
                    errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.key.required`);
                else if (!/^\w+$/.test(permission.key)) {
                    errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.key.invalid`);
                }

                if (!permission.label)
                    errors.push(`organisation.groups.${indexGroup}.permissions.${indexPermission}.label.required`);
            })
        });

        if (nextState.organisation.offers)
        nextState.organisation.offers.forEach((offer, indexOffer) => {
            if (!offer.key)
                errors.push(`organisation.offers.${indexOffer}.key.required`);
            else if (!/^\w+$/.test(offer.key)) {
                errors.push(`organisation.offers.${indexOffer}.key.invalid`);
            }

            if (!offer.label)
                errors.push(`organisation.offers.${indexOffer}.label.required`);

            offer.groups.forEach((group, indexGroup) => {
                if (!group.key)
                    errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.key.required`);
                else if (!/^\w+$/.test(group.key)) {
                    errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.key.invalid`);
                }

                if (!group.label)
                    errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.label.required`);

                group.permissions.forEach((permission, indexPermission) => {
                    if (!permission.key)
                        errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.key.required`);
                    else if (!/^\w+$/.test(permission.key)) {
                        errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.key.invalid`);
                    }

                    if (!permission.label)
                        errors.push(`organisation.offers.${indexOffer}.groups.${indexGroup}.permissions.${indexPermission}.label.required`);
                })
            })
        });

        this.setState({errors});
        console.log("errors ", errors);
        return errors.length === 0;
    };

    save = () => {
        if (this.validate(this.state) && !this.props.readOnlyMode) {
            const organisation = {...this.state.organisation};

            if (organisation.version)
                delete organisation.version;

            if (this.props.organisationKey)
                organisationService.saveOrganisationDraft(this.props.tenant, this.props.organisationKey, organisation)
                    .then(() => {
                        if (this.props.onSave)
                            this.props.onSave();

                        if (this.props.reloadAfterSave)
                            this.fetch(this.props);
                    });
            else
                organisationService.createOrganisation(this.props.tenant, organisation)
                    .then(() => {
                        if (this.props.onSave)
                            this.props.onSave();

                        if (this.props.reloadAfterSave)
                            this.fetch(this.props);
                    });
        }
    };

    release = () => {
        if (this.validate(this.state) && !this.props.readOnlyMode) {
            const organisation = {...this.state.organisation};

            if (organisation.offers && !organisation.offers.length)
                delete organisation.offers;

            if (this.props.organisationKey) {
                if (organisation.version)
                    delete organisation.version;

                organisationService.saveOrganisationDraft(this.props.tenant, this.props.organisationKey, organisation)
                    .then(() => {
                        organisationService.createOrganisationRelease(this.props.tenant, this.props.organisationKey)
                            .then(() => {
                                if (this.props.onSave)
                                    this.props.onSave();

                                if (this.props.reloadAfterSave)
                                    this.fetch(this.props);
                            });
                    });
            }
            else {
                if (organisation.version)
                    delete organisation.version;

                organisationService.createOrganisation(this.props.tenant, organisation)
                    .then(organisationCreated => {
                        return organisationService.createOrganisationRelease(this.props.tenant, this.state.organisation.key)
                            .then(() => {
                                if (this.props.onSave)
                                    this.props.onSave();

                                if (this.props.reloadAfterSave)
                                    this.fetch(this.props);
                            });
                    });
            }
        }
    };

    cancel = () => {
        if (!this.props.readOnlyMode)
            this.fetch();
    };

    toggleVisualize = () => {
        this.setState({visualizeConsents: !this.state.visualizeConsents});
    };

    render() {
        if (this.state.loading) {
            return "Loading";
        }

        const actionButtons = (
            <div className="form-buttons pull-right btnsNewOrga">
                <button className="btn btn-danger" title="cancel" onClick={this.cancel}><i
                    className="glyphicon glyphicon-remove"/></button>

                <button className="btn btn-primary" title="display consents" onClick={this.toggleVisualize}>
                    {
                        this.state.visualizeConsents ?
                            <i className="glyphicon glyphicon-eye-close"/>
                            :
                            <i className="glyphicon glyphicon-eye-open"/>
                    }
                </button>
                <button className="btn btn-success" title="define as currently version" onClick={this.release}>
                    Définir comme version courante
                </button>
                <button className="btn btn-success" title="save" onClick={this.save}>
                    {
                        this.props.organisationKey ?
                            <i className="glyphicon glyphicon-hdd"/>
                            :
                            <i className="fa fa-floppy-o"/>
                    }
                </button>
            </div>

        );

        return (
            <div className="row">
                {
                    !this.props.organisationKey &&
                    <div className="col-md-12">
                        <h3>Nouvelle organisation</h3>
                    </div>
                }

                {
                    !this.state.haveReleases &&
                    <div className="col-md-12">
                        <div className="alert alert-warning">
                            <h4 className="alert-heading">Attention</h4>
                            Aucune version n'a été publiée pour cette organisation.
                        </div>
                    </div>
                }

                <div className="col-md-12 blocOrganisation">
                    {
                        this.props.readOnlyMode ?
                            <LabelInput label={"Version"} value={this.state.organisation.version.num || "1"}/>
                            :
                            <LabelInput label={"Future version"} value={this.state.organisation.version.num || "1"}/>
                    }

                    <TextInput
                        label={"Clé de l'organisation"}
                        value={this.state.organisation.key}
                        onChange={(e) => this.onChange(e, "key")}
                        disabled={this.props.readOnlyMode ? true : !!this.props.organisationKey}
                        errorKey={["organisation.key.required", "organisation.key.invalid"]}
                        errorMessage={this.state.errors}
                    />
                    <TextInput
                        label={"Libellé de l'organisation"}
                        value={this.state.organisation.label}
                        onChange={(e) => this.onChange(e, "label")}
                        disabled={this.props.readOnlyMode}
                        errorKey="organisation.label.required"
                        errorMessage={this.state.errors}
                    />
                </div>

                <div className="col-md-12">
                    <div className="row">
                        <div className="col-md-12" style={{'marginTop': '20px'}}>
                            {
                                !this.props.readOnlyMode &&
                                <div className="btn btn-xs btn-primary pull-right" onClick={this.addGroup}>Créer un
                                    groupe
                                </div>
                            }
                        </div>
                    </div>
                    {
                        this.state.organisation.groups.map((group, index) =>
                            <Group key={index} index={index} group={group} onChange={this.onChangeGroup}
                                   onRemove={() => this.removeGroup(index)} readOnlyMode={this.props.readOnlyMode}
                                   prefixe={"organisation."} errors={this.state.errors}/>
                        )
                    }
                </div>

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
                            this.state.organisation.offers && this.state.organisation.offers.map(
                                (offer, index) =>
                                    <Offer key={index} index={index} offer={offer} onChange={this.onChangeOffer}
                                           onRemove={() => this.removeOffer(index)} errors={this.state.errors}
                                           prefixe={"organisation."} readOnlyMode={this.state.readOnlyMode}/>
                            )
                        }
                    </div>
                    {!this.props.readOnlyMode && actionButtons}
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
                    this.state.visualizeConsents &&
                    <div className="col-md-12">
                        <h3>Pré-visualisation</h3>
                    </div>
                }

                {
                    this.state.visualizeConsents &&
                    <ConsentsPage groups={
                        [
                            ...this.state.organisation.groups.map(group => {
                                return {
                                    label: group.label,
                                    consents: group.permissions.map(permission => {
                                        return {
                                            key: permission.key,
                                            label: permission.label,
                                            checked: false
                                        }
                                    })
                                }
                            })
                        ]
                    }
                    offers={[...this.state.organisation.offers.map(offer => ({
                        key: offer.key,
                        label: offer.label,
                        groups: offer.groups.map(group => ({
                            key: group.key,
                            label: group.label,
                            consents: group.permissions.map(permission => ({
                                    key: permission.key,
                                    label: permission.label,
                                    checked: false
                                })
                            )
                        }))

                    }))]}/>
                }
            </div>
        );
    }
}

OrganisationPage.propTypes = {
    tenant: PropTypes.string.isRequired,
    organisationKey: PropTypes.string,
    onSave: PropTypes.func,
    reloadAfterSave: PropTypes.bool,
    readOnlyMode: PropTypes.bool,
    version: PropTypes.any
};

class Offer extends Component {

    state = {
        offer: {
            name: '',
            groups: []
        }
    };

    componentDidMount() {
        this.setState({offer: this.props.offer});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({offer: nextProps.offer});
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

            const groups = [...this.state.offer.groups];
            groups.unshift(group);

            this.setState({offer: {...this.state.offer, groups}});
        }
    };

    removeGroup = (index) => {
        if (!this.props.readOnlyMode) {
            const groups = [...this.state.offer.groups];
            groups.splice(index, 1);
            this.setState({offer: {...this.state.offer, groups}});
        }
    };

    onChangeGroup = (index, group) => {
        if (!this.props.readOnlyMode) {
            const groups = [...this.state.offer.groups];
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
                            <Group key={index} index={index} group={group} onChange={this.onChangeGroup}
                                   onRemove={() => this.removeGroup(index)} readOnlyMode={this.props.readOnlyMode}
                                   prefixe={`${this.props.prefixe}offers.${this.props.index}.`} errors={this.props.errors}/>
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

class Group extends Component {

    state = {
        group: {
            key: '',
            label: '',
            permissions: []
        }
    };

    componentDidMount() {
        this.setState({group: this.props.group});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({group: nextProps.group});
    }

    addPermission = () => {
        if (!this.props.readOnlyMode) {
            const permission = {
                key: '',
                label: ''
            };

            const permissions = [...this.state.group.permissions];
            permissions.unshift(permission);

            this.setState({group: {...this.state.group, permissions}}, () => {
                this.props.onChange(this.props.index, this.state.group);
            });
        }
    };

    removePermission = (index) => {
        if (!this.props.readOnlyMode) {
            const permissions = [...this.state.group.permissions];
            permissions.splice(index, 1);
            this.setState({group: {...this.state.group, permissions}}, () => {
                this.props.onChange(this.props.index, this.state.group);
            });
        }
    };

    onChange = (value, name) => {
        if (!this.props.readOnlyMode)
            this.setState({group: {...this.state.group, [name]: value}}, () => {
                this.props.onChange(this.props.index, this.state.group);
            });
    };

    onChangePermission = (index, permission) => {
        if (!this.props.readOnlyMode) {
            const permissions = [...this.state.group.permissions];
            permissions[index] = permission;
            this.setState({group: {...this.state.group, permissions}}, () => {
                this.props.onChange(this.props.index, this.state.group);
            });
        }
    };

    render() {
        return (

            <div className="groupContent">
                <hr/>
                <div className="form-group">
                    <div className="row">
                        <label className="col-sm-2 control-label"/>
                        <div className="col-sm-10">
                            <span className="groupe">Groupe</span>
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

                <TextInput label={"Clé du groupe"} value={this.state.group.key}
                           onChange={(e) => this.onChange(e, "key")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={[`${this.props.prefixe}groups.${this.props.index}.key.required`, `${this.props.prefixe}groups.${this.props.index}.key.invalid`]}/>
                <TextInput label={"Libellé du groupe"} value={this.state.group.label}
                           onChange={(e) => this.onChange(e, "label")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={`${this.props.prefixe}groups.${this.props.index}.label.required`}/>

                <div className="form-group">
                    <div className="row">
                        <label className="col-sm-2 control-label"/>
                        <div className="col-sm-10" style={{'marginTop': '40px'}}>
                            <span className="groupe">Permissions</span>
                            {
                                !this.props.readOnlyMode &&
                                <button type="button" className="btn btn-primary btn-xs" style={{'marginLeft': '10px'}}
                                        onClick={this.addPermission}>
                                    <i className="glyphicon glyphicon-plus"/>
                                </button>
                            }
                        </div>
                    </div>
                </div>


                {
                    this.state.group.permissions.map((permission, index) =>
                        <Permission key={index} index={index} permission={permission} onChange={this.onChangePermission}
                                    onRemove={() => this.removePermission(index)} readOnlyMode={this.props.readOnlyMode}
                                    errors={this.props.errors}
                                    prefixe={`${this.props.prefixe}groups.${this.props.index}.`}/>
                    )
                }
            </div>
        );
    }
}

Group.propTypes = {
    group: PropTypes.object.isRequired,
    index: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired,
    readOnlyMode: PropTypes.bool,
    prefixe: PropTypes.string,
    errors: PropTypes.array
};


class Permission extends Component {

    state = {
        permission: {
            key: '',
            label: ''
        }
    };

    componentDidMount() {
        this.setState({permission: this.props.permission});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({permission: nextProps.permission});
    }

    onChange = (value, name) => {
        if (!this.props.readOnlyMode)
            this.setState({permission: {...this.state.permission, [name]: value}}, () => {
                this.props.onChange(this.props.index, this.state.permission);
            });
    };

    render() {
        return (
            <div className="blocOnePermission">
                <div className="form-group">
                    <div className="row">
                        <label className="col-sm-2 control-label"/>
                        <div className="col-sm-10" style={{'marginTop': '10px'}}>
                            {
                                !this.props.readOnlyMode &&
                                <button className="btn btn-danger pull-right btn-xs" onClick={this.props.onRemove}>
                                    <i className="glyphicon glyphicon-trash"/>
                                </button>
                            }
                        </div>
                    </div>
                </div>
                <TextInput label={"Clé de la permission"} value={this.state.permission.key}
                           onChange={(e) => this.onChange(e, "key")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={[`${this.props.prefixe}permissions.${this.props.index}.key.required`, `${this.props.prefixe}permissions.${this.props.index}.key.invalid`]}/>

                <TextInput label={"Libellé de la permission"} value={this.state.permission.label}
                           onChange={(e) => this.onChange(e, "label")}
                           disabled={this.props.readOnlyMode}
                           errorMessage={this.props.errors}
                           errorKey={`${this.props.prefixe}permissions.${this.props.index}.label.required`}/>

            </div>
        );
    }
}

Permission.propTypes = {
    permission: PropTypes.object.isRequired,
    index: PropTypes.number.isRequired,
    onChange: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired,
    readOnlyMode: PropTypes.bool,
    prefixe: PropTypes.string,
    errors: PropTypes.array
};
