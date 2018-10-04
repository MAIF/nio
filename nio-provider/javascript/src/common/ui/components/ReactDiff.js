import React, {Component} from 'react';
import PropTypes from "prop-types";
import * as jsdiff from 'diff';

export class ReactDiff extends Component {
  fnMap = {
    'chars': jsdiff.diffChars,
    'words': jsdiff.diffWords,
    'sentences': jsdiff.diffSentences,
    'json': jsdiff.diffJson
  };

  state = {
    inputA: null,
    inputB: null,
    type: 'json',
    format: 'splited'
  };

  componentDidMount() {
    this.setState({
      inputA: this.props.inputA,
      inputB: this.props.inputB,
      type: this.props.type || 'json',
      format: this.props.format || 'splited'
    });
  }

  componentWillReceiveProps(nextProps) {
    this.setState({
      inputA: nextProps.inputA,
      inputB: nextProps.inputB,
      type: nextProps.type || 'json',
      format: nextProps.format || 'splited'
    });
  }

  render() {
    const diff = this.fnMap[this.props.type](this.props.inputA, this.props.inputB);

    const result = [];
    const resultA = [];
    const resultB = [];

    diff.forEach((part, index) => {
      let spanStyle = {
        backgroundColor: part.added ? 'lightgreen' : part.removed ? 'salmon' : ''
      };

      const element = (
        <span key={index} style={spanStyle}>{part.added ? "+ " : part.removed ? "- " : ""} {part.value}</span>
      );

      if (!part.added)
        resultA.push(element);

      if (!part.removed)
        resultB.push(element);

      result.push(element);
    });

    if ("unified" === this.state.format) {
      return (
        <div className="row">
        <pre className="col-md-12 diff-result">
          {result}
        </pre>
        </div>
      );
    }

    return (
      <div className="row">
        <pre className="col-md-6 diff-result">
          {resultA}
        </pre>
        <pre className="col-md-6 diff-result">
          {resultB}
        </pre>
      </div>
    );
  }
}

ReactDiff.propTypes = {
  inputA: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
  inputB: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
  type: PropTypes.oneOf(["chars", "words", "sentences", "json"]),
  format: PropTypes.oneOf(["unified", "splited"])
};