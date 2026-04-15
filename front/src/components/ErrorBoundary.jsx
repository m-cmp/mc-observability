import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }
  static getDerivedStateFromError(error) {
    return { error };
  }
  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info);
  }
  render() {
    if (this.state.error) {
      return (
        <div className="p-4 bg-red-50 border border-red-200 rounded m-4">
          <h3 className="text-red-700 font-bold mb-2">Render Error</h3>
          <pre className="text-xs text-red-600 whitespace-pre-wrap">{this.state.error.toString()}</pre>
          <button onClick={() => this.setState({ error: null })} className="mt-2 text-sm bg-red-600 text-white px-3 py-1 rounded">Retry</button>
        </div>
      );
    }
    return this.props.children;
  }
}
