import React, {Component} from 'react';
import {Help} from './Help';
import PropTypes from "prop-types";

export class LabelInput extends Component {

  render() {
    return (
      <div className="form-group">
        <label htmlFor={`input-${this.props.label}`} className={`col-xs-12 col-sm-2 control-label`} style={this.props.alignLeft ? {textAlign: "left"}: {}}>
          {this.props.label} <Help text={this.props.help}/>
        </label>

        <div className="col-sm-10">
          <label htmlFor={`input-${this.props.value}`} className="col-xs-12 col-sm-2 control-label-left">
            {this.props.value} <Help text={this.props.help}/>
          </label>
        </div>
      </div>
    );
  }
}

LabelInput.propTypes = {
  label: PropTypes.string,
  value: PropTypes.any,
  help: PropTypes.string,
  alignLeft: PropTypes.bool,
};
