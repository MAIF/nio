import {Component} from "react";
import _ from "lodash";
import {TextInput} from "../../common/ui/inputs";
import PropTypes from "prop-types";
import React from "react";

export class GroupPermissionPage extends Component {

    state = {
        group: {
            key: '',
            label: '',
            permissions: []
        }
    };

    componentDidMount() {
        this.setState({group: _.cloneDeep(this.props.group)});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({group: _.cloneDeep(nextProps.group)});
    }

    addPermission = () => {
        if (!this.props.readOnlyMode) {
            const permission = {
                key: '',
                label: ''
            };

            const permissions = _.cloneDeep(this.state.group.permissions);
            permissions.unshift(permission);

            this.setState({group: {...this.state.group, permissions}}, () => {
                this.props.onChange(this.props.index, this.state.group);
            });
        }
    };

    removePermission = (index) => {
        if (!this.props.readOnlyMode) {
            const permissions = _.cloneDeep(this.state.group.permissions);
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
            const permissions = _.cloneDeep(this.state.group.permissions);
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

GroupPermissionPage.propTypes = {
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
        this.setState({permission: _.cloneDeep(this.props.permission)});
    }

    componentWillReceiveProps(nextProps) {
        this.setState({permission: _.cloneDeep(nextProps.permission)});
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
