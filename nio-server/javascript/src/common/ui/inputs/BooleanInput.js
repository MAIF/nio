import React, {Component} from 'react';
import {Help} from './Help';
import PropTypes from "prop-types";

const OnSwitch = props => (
    <div className="content-switch-button-on" onClick={props.onChange}>
        <div className="switch-button-on"/>
    </div>
);

const OffSwitch = props => (
    <div className="content-switch-button-off" onClick={props.onChange}>
        <div className="switch-button-off"/>
    </div>
);

export class BooleanInput extends Component {
    toggleOff = e => {
        if (e && e.preventDefault) e.preventDefault();
        this.props.onChange(false);
    };

    toggleOn = e => {
        if (e && e.preventDefault) e.preventDefault();
        this.props.onChange(true);
    };

    toggle = value => {
        this.props.onChange(value);
    };

    render() {
        const value = !!this.props.value;

        return (
            <div>
                <div className="form-group">
                    <label className="col-xs-12 col-sm-2 control-label">
                        {this.props.label} <Help text={this.props.help}/>
                    </label>
                    <div className="col-sm-10">
                        <div className="row">
                            <div className="col-sm-6">
                                {value && <OnSwitch onChange={this.toggleOff}/>}
                                {!value && <OffSwitch onChange={this.toggleOn}/>}
                            </div>
                            <div className="col-sm-6">
                                {this.props.after && <div className="pull-right">{this.props.after()}</div>}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

BooleanInput.propTypes = {
    after: PropTypes.func,
    label: PropTypes.string,
    help: PropTypes.string,
    value: PropTypes.bool,
    onChange: PropTypes.func
};
