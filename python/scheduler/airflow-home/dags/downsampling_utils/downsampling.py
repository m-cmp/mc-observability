import pandas as pd
import numpy as np


def weighted_moving_average(data: pd.DataFrame, col_names: list, weights=None):
    if weights is None:
        weights = [0.2, 0.3, 0.5]
    weights_np = np.array(weights)
    data_modified = data.copy()

    numeric_cols = data.select_dtypes(include=[np.number]).columns.intersection(col_names)

    for col_name in numeric_cols:
        if col_name in data.columns:
            data_target = data[col_name]
            wma = data_target.rolling(window=len(weights_np)).apply(
                lambda x: (x * weights_np).sum() / weights_np.sum(), raw=True
            )
            wma[:len(weights_np) - 1] = data_target[:len(weights_np) - 1]
            data_modified[col_name] = wma

    return data_modified.round(4)



def data_reduction(data: pd.DataFrame, col_names: list, cut_size: int):
    data = data.sort_values(by='time')
    data = data.reset_index(drop=True)
    data['group'] = data.index // cut_size

    agg_dict = {'time': 'last'}
    numeric_cols = data.select_dtypes(include=[np.number]).columns.intersection(col_names)

    for col in numeric_cols:
        agg_dict[col] = 'mean'

    result = data.groupby('group').agg(agg_dict).reset_index(drop=True)
    return result.round(4)
